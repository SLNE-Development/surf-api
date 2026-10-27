package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.DescFieldMessageList
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage

internal class MessageListStep(
    private val field: DescFieldMessageList,
) : FieldStep(validatesOutgoing = true) {

    override fun apply(message: ReflectMessage, direction: Direction): Violation? {
        val plan = MessagePlan.of(field.message)
        if (plan.isNoOp(direction)) return null

        val list = message.get(field)
        for (index in 0 until list.size) {
            val violation = plan.apply(list.get(index)!!, direction)
            if (violation != null) {
                return violation.under("${field.localName}[$index]")
            }
        }

        return null
    }
}
