package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.external.protobuf.DescEnum
import dev.slne.surf.nats.js.external.protobuf.DescFieldEnum
import dev.slne.surf.nats.js.external.protobuf.DescFieldEnumList
import dev.slne.surf.nats.js.external.protobuf.DescFieldEnumMap
import dev.slne.surf.nats.js.external.protobuf.DescFieldMessage
import dev.slne.surf.nats.js.external.protobuf.DescFieldMessageList
import dev.slne.surf.nats.js.external.protobuf.DescFieldMessageMap
import dev.slne.surf.nats.js.external.protobuf.DescFieldScalar
import dev.slne.surf.nats.js.external.protobuf.DescFieldSingular
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.create
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.reflect
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import js.collections.WeakMap
import js.iterable.JsIterator
import js.iterable.isYield
import js.typedarrays.Uint8Array

/**
 * Applies the presence and enum rules of the Kotlin codec to protobuf-es messages.
 *
 * A field is required when it has explicit presence, is no oneof member, has no `(surf.nats.v1.nullable)` option
 * and no proto `[default]`; Kotlin fails to decode a message without it. `google.protobuf.*` messages are not
 * checked. `surf.types.v1.Uuid.value` must be exactly 16 bytes.
 *
 * Kotlin decodes unknown numbers of closed enums to the zero entry in every position. protobuf-es keeps them as
 * raw numbers in lists and maps and moves singular values to `$unknown`; [normalizeAndCheck] rewrites both to the
 * zero entry and drops the moved values from `$unknown`.
 *
 * The checks are planned once per `DescMessage` and run in field declaration order; the reported violation is the
 * first in that order.
 */
internal object MessageRules {
    private const val PRESENCE_IMPLICIT = 2

    // Checks of a set scalar field of the message type; a check returns the problem or null.
    private val typeRules: Map<String, (Any) -> String?> = mapOf(
        ProtocolFieldRules.UUID_TYPE to { value ->
            if (value is Uint8Array<*> && value.length != ProtocolFieldRules.UUID_BYTES) {
                "must be exactly ${ProtocolFieldRules.UUID_BYTES} bytes, was ${value.length}"
            } else {
                null
            }
        },
    )

    private val plans = WeakMap<DescMessage, MessagePlan>()
    private val enums = WeakMap<DescEnum, EnumPlan>()

    // Accepts a message of [schema] or an init object for create(); throws on a rule violation.
    fun prepare(schema: DescMessage, value: ProtoMessageInit?): ProtoMessage {
        val message = when {
            value == null -> create(schema)
            else -> messageOf(schema, value) ?: run {
                val typeName = value.`$typeName`
                require(typeName == null) { "Expected a ${schema.typeName} message, got $typeName" }
                create(schema, value)
            }
        }
        violation(schema, message)?.let { throw IllegalArgumentException("Invalid ${schema.typeName}: $it") }
        return message
    }

    // The first rule violation of [message] as a sentence naming the field path, or null.
    fun violation(schema: DescMessage, message: ProtoMessage): String? = check(schema, message, normalize = false)

    // Normalises the enums of a received [message] and returns its first rule violation like [violation].
    fun normalizeAndCheck(schema: DescMessage, message: ProtoMessage): String? = check(schema, message, normalize = true)

    private fun check(schema: DescMessage, message: ProtoMessage, normalize: Boolean): String? {
        val plan = plan(schema)
        if (plan.skips(normalize)) return null
        return plan.apply(reflect(schema, message), normalize)?.toString()
    }

    private fun plan(schema: DescMessage): MessagePlan {
        plans.get(schema)?.let { return it }
        return MessagePlan(steps(schema)).also { plans.set(schema, it) }
    }

    // Nested message types are planned when the walk first reaches them, so recursive types need no special case.
    private fun steps(schema: DescMessage): Array<Step> {
        if (schema.typeName.startsWith(ProtocolFieldRules.WELL_KNOWN_PREFIX)) return emptyArray()

        val rule = typeRules[schema.typeName]
        val steps = ArrayList<Step>()
        for (field in schema.fields) {
            when (val shape = field.shape()) {
                is FieldShape.Scalar -> {
                    val required = isRequired(shape.field)
                    if (required || rule != null) steps += ScalarStep(shape.field, required, rule)
                }

                is FieldShape.Enum -> steps += EnumStep(shape.field, enumPlan(shape.field.enum), isRequired(shape.field))
                is FieldShape.Message -> steps += MessageStep(shape.field, isRequired(shape.field))
                is FieldShape.EnumList -> steps += EnumListStep(shape.field, enumPlan(shape.field.enum))
                is FieldShape.MessageList -> steps += MessageListStep(shape.field)
                is FieldShape.EnumMap -> steps += EnumMapStep(shape.field, enumPlan(shape.field.enum))
                is FieldShape.MessageMap -> steps += MessageMapStep(shape.field)
                FieldShape.Scalars -> Unit
            }
        }
        return steps.toTypedArray()
    }

    private fun enumPlan(enum: DescEnum): EnumPlan {
        enums.get(enum)?.let { return it }
        return EnumPlan(enum).also { enums.set(enum, it) }
    }

    private fun isRequired(field: DescFieldSingular): Boolean {
        return ProtocolFieldRules.isRequired(
            explicitPresence = field.presence != PRESENCE_IMPLICIT,
            oneofMember = field.oneof != null,
            nullable = Contracts.isNullable(field),
            hasDefault = field.getDefaultValue() != null,
        )
    }

    // Applies the plan of [type] to the nested message returned by [message], unless the plan has nothing to do.
    private inline fun nested(type: DescMessage, normalize: Boolean, message: () -> ReflectMessage): Violation? {
        val plan = plan(type)
        return if (plan.skips(normalize)) null else plan.apply(message(), normalize)
    }

    private inline fun <K : Any> JsIterator<K>.forEachKey(action: (K) -> Unit) {
        while (true) {
            val result = next()
            if (!isYield(result)) return
            action(result.value)
        }
    }

    /**
     * A rule violation; the field path grows from the innermost field outwards while the walk returns.
     *
     * [problem] is `null` for a required field that is not set.
     */
    private class Violation(private var path: String, private val problem: String?) {
        fun under(segment: String): Violation = apply { path = "$segment.$path" }

        override fun toString(): String = problem?.let { "field '$path' $it" } ?: "required field '$path' is not set"
    }

    private class EnumPlan(enum: DescEnum) {
        private val known: Set<Int> = enum.values.mapTo(HashSet()) { it.number }
        val zero: Int = (enum.values.firstOrNull { it.number == 0 } ?: enum.values.first()).number

        fun isKnown(number: Int): Boolean = number in known
    }

    private class MessagePlan(private val steps: Array<Step>) {
        private val validates = steps.any { it.validates }

        // Whether a walk would neither normalise nor validate anything.
        fun skips(normalize: Boolean): Boolean = if (normalize) steps.isEmpty() else !validates

        fun apply(message: ReflectMessage, normalize: Boolean): Violation? {
            for (step in steps) {
                step.apply(message, normalize)?.let { return it }
            }
            return null
        }
    }

    private sealed class Step(val validates: Boolean) {
        abstract fun apply(message: ReflectMessage, normalize: Boolean): Violation?
    }

    private class ScalarStep(
        private val field: DescFieldScalar,
        private val required: Boolean,
        private val rule: ((Any) -> String?)?,
    ) : Step(validates = true) {
        override fun apply(message: ReflectMessage, normalize: Boolean): Violation? {
            if (!message.isSet(field)) return if (required) Violation(field.localName, null) else null
            val problem = rule?.invoke(message.get(field)) ?: return null
            return Violation(field.localName, problem)
        }
    }

    private class EnumStep(
        private val field: DescFieldEnum,
        private val enum: EnumPlan,
        private val required: Boolean,
    ) : Step(validates = required) {
        override fun apply(message: ReflectMessage, normalize: Boolean): Violation? {
            if (normalize) normalizeSingular(message)
            return if (required && !message.isSet(field)) Violation(field.localName, null) else null
        }

        private fun normalizeSingular(message: ReflectMessage) {
            if (message.isSet(field)) {
                // Open enums keep unknown numbers in the field itself.
                if (!enum.isKnown(message.get(field))) message.set(field, enum.zero)
                return
            }

            val unknown = message.getUnknown() ?: return
            if (unknown.none { it.no == field.number }) return

            val oneof = field.oneof
            if (oneof == null || message.oneofCase(oneof) == null) message.set(field, enum.zero)

            val remaining = unknown.filter { it.no != field.number }
            message.setUnknown(if (remaining.isEmpty()) undefined else remaining.toTypedArray())
        }
    }

    private class MessageStep(private val field: DescFieldMessage, private val required: Boolean) : Step(validates = true) {
        override fun apply(message: ReflectMessage, normalize: Boolean): Violation? {
            if (!message.isSet(field)) return if (required) Violation(field.localName, null) else null
            return nested(field.message, normalize) { message.get(field) }?.under(field.localName)
        }
    }

    private class EnumListStep(private val field: DescFieldEnumList, private val enum: EnumPlan) : Step(validates = false) {
        override fun apply(message: ReflectMessage, normalize: Boolean): Violation? {
            if (!normalize) return null
            val list = message.get(field)
            for (index in 0 until list.size) {
                if (!enum.isKnown(list.get(index)!!)) list.set(index, enum.zero)
            }
            return null
        }
    }

    private class MessageListStep(private val field: DescFieldMessageList) : Step(validates = true) {
        override fun apply(message: ReflectMessage, normalize: Boolean): Violation? {
            val plan = plan(field.message)
            if (plan.skips(normalize)) return null
            val list = message.get(field)
            for (index in 0 until list.size) {
                plan.apply(list.get(index)!!, normalize)?.let { return it.under("${field.localName}[$index]") }
            }
            return null
        }
    }

    // Map values are rewritten while iterating the keys: ReflectMap.keys() iterates a snapshot of the keys.
    private class EnumMapStep(private val field: DescFieldEnumMap, private val enum: EnumPlan) : Step(validates = false) {
        override fun apply(message: ReflectMessage, normalize: Boolean): Violation? {
            if (!normalize) return null
            val map = message.get(field)
            map.keys().forEachKey { key ->
                if (!enum.isKnown(map.get(key)!!)) map.set(key, enum.zero)
            }
            return null
        }
    }

    private class MessageMapStep(private val field: DescFieldMessageMap) : Step(validates = true) {
        override fun apply(message: ReflectMessage, normalize: Boolean): Violation? {
            val plan = plan(field.message)
            if (plan.skips(normalize)) return null
            val map = message.get(field)
            map.keys().forEachKey { key ->
                plan.apply(map.get(key)!!, normalize)?.let { return it.under("${field.localName}[$key]") }
            }
            return null
        }
    }
}
