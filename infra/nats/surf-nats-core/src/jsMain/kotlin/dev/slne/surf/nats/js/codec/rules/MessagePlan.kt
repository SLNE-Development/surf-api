package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.descriptor.ProtocolOptionReader
import dev.slne.surf.nats.js.external.protobuf.DescFieldSingular
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import dev.slne.surf.nats.js.interop.FieldShape
import dev.slne.surf.nats.js.interop.getOrPut
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import js.collections.WeakMap

/**
 * The field steps of one message type, built once per [DescMessage].
 */
internal class MessagePlan private constructor(
    private val incomingSteps: Array<FieldStep>,
    private val outgoingSteps: Array<FieldStep>,
) {

    fun isNoOp(direction: Direction): Boolean = when (direction) {
        Direction.OUTGOING -> outgoingSteps.isEmpty()
        Direction.INCOMING -> incomingSteps.isEmpty()
    }

    fun apply(message: ReflectMessage, direction: Direction): Violation? {
        val steps = when (direction) {
            Direction.OUTGOING -> outgoingSteps
            Direction.INCOMING -> incomingSteps
        }

        for (step in steps) {
            val violation = step.apply(message, direction)
            if (violation != null) return violation
        }

        return null
    }

    companion object {
        private const val PRESENCE_IMPLICIT = 2

        private val plans = WeakMap<DescMessage, MessagePlan>()

        /**
         * Nested message types are planned when a walk first reaches them, so recursive types need no special case.
         */
        fun of(schema: DescMessage): MessagePlan = plans.getOrPut(schema) {
            val steps = steps(schema)
            MessagePlan(
                incomingSteps = steps,
                outgoingSteps = steps.filter { it.validatesOutgoing }.toTypedArray(),
            )
        }

        private fun steps(schema: DescMessage): Array<FieldStep> {
            if (schema.typeName.startsWith(ProtocolFieldRules.WELL_KNOWN_PREFIX)) {
                return emptyArray()
            }

            val rule = ScalarRule.forType(schema.typeName)
            return schema.fields.mapNotNull { field ->
                when (val shape = FieldShape.of(field)) {
                    is FieldShape.Scalar -> {
                        val required = isRequired(shape.field)
                        if (required || rule != null) {
                            ScalarStep(shape.field, required, rule)
                        } else {
                            null
                        }
                    }

                    is FieldShape.Enum -> EnumStep(
                        shape.field,
                        EnumPlan.of(shape.field.enum),
                        isRequired(shape.field)
                    )

                    is FieldShape.Message -> MessageStep(shape.field, isRequired(shape.field))

                    is FieldShape.EnumList -> EnumListStep(
                        shape.field,
                        EnumPlan.of(shape.field.enum)
                    )

                    is FieldShape.MessageList -> MessageListStep(shape.field)
                    is FieldShape.EnumMap -> EnumMapStep(shape.field, EnumPlan.of(shape.field.enum))
                    is FieldShape.MessageMap -> MessageMapStep(shape.field)
                    FieldShape.ScalarListOrMap -> null
                }
            }.toTypedArray()
        }

        private fun isRequired(field: DescFieldSingular): Boolean {
            return ProtocolFieldRules.isRequired(
                explicitPresence = field.presence != PRESENCE_IMPLICIT,
                oneofMember = field.oneof != null,
                nullable = ProtocolOptionReader.isNullable(field),
                hasDefault = field.getDefaultValue() != null,
            )
        }
    }
}
