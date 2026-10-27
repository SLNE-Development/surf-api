package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.interop.messageOf
import dev.slne.surf.nats.js.external.protobuf.create as createMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.reflect as reflectMessage

/**
 * Applies the presence and enum rules of the Kotlin codec to protobuf-es messages.
 *
 * A field is required when it has explicit presence, is no oneof member, has no `(surf.nats.v1.nullable)` option
 * and no proto `[default]`; Kotlin fails to decode a message without it. `google.protobuf.*` messages are not
 * checked. `surf.types.v1.Uuid.value` must be exactly 16 bytes.
 *
 * Kotlin decodes unknown numbers of closed enums to the zero entry in every position. protobuf-es keeps them as
 * raw numbers in lists and maps and moves singular values to `$unknown`; incoming messages are rewritten to the
 * zero entry.
 *
 * The reported violation is the first in field declaration order.
 */
internal object MessageRules {

    /**
     * Returns [value] as a message of [schema], creating it from an init object or `null`.
     */
    fun prepare(schema: DescMessage, value: ProtoMessageInit?): ProtoMessage {
        val message = toMessage(schema, value)
        val violation = violation(schema, message)

        require(violation == null) {
            "Invalid ${schema.typeName}: $violation"
        }

        return message
    }

    fun violation(
        schema: DescMessage,
        message: ProtoMessage,
    ): String? = check(schema, message, Direction.OUTGOING)

    fun normalizeAndCheck(schema: DescMessage, message: ProtoMessage): String? {
        return check(schema, message, Direction.INCOMING)
    }

    private fun toMessage(schema: DescMessage, value: ProtoMessageInit?): ProtoMessage {
        if (value == null) return createMessage(schema)

        messageOf(schema, value)?.let { return it }

        val typeName = value.`$typeName`
        require(typeName == null) {
            "Expected a ${schema.typeName} message, got $typeName"
        }

        return createMessage(schema, value)
    }

    private fun check(schema: DescMessage, message: ProtoMessage, direction: Direction): String? {
        val plan = MessagePlan.of(schema)
        if (plan.isNoOp(direction)) return null

        return plan.apply(reflectMessage(schema, message), direction)?.toString()
    }
}
