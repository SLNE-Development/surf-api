package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.DescFieldMessage
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage

internal class MessageStep(
    private val field: DescFieldMessage,
    private val required: Boolean,
) : FieldStep(validatesOutgoing = true) {

    override fun apply(message: ReflectMessage, direction: Direction): Violation? {
        if (!message.isSet(field)) {
            return if (required) Violation.missing(field.localName) else null
        }

        val plan = MessagePlan.of(field.message)
        if (plan.isNoOp(direction)) return null

        return plan.apply(message.get(field), direction)?.under(field.localName)
    }
}
