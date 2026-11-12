package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.DescFieldMessageMap
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import js.iterable.iterator

/**
 * Applies the plan of the value type to every message of a map and reports the path with the key.
 */
internal class MessageMapStep(
    private val field: DescFieldMessageMap,
) : FieldStep(validatesOutgoing = true) {

    override fun apply(message: ReflectMessage, direction: Direction): Violation? {
        val plan = MessagePlan.of(field.message)
        if (plan.isNoOp(direction)) return null

        val map = message.get(field)
        for (key in map.keys()) {
            val violation = plan.apply(map.get(key)!!, direction)
            if (violation != null) {
                return violation.under("${field.localName}[$key]")
            }
        }

        return null
    }
}
