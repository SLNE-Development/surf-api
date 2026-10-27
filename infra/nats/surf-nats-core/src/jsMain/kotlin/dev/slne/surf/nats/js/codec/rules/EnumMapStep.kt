package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.DescFieldEnumMap
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import js.iterable.iterator

internal class EnumMapStep(
    private val field: DescFieldEnumMap,
    private val enum: EnumPlan,
) : FieldStep(validatesOutgoing = false) {

    override fun apply(message: ReflectMessage, direction: Direction): Violation? {
        if (direction == Direction.OUTGOING) return null

        val map = message.get(field)
        for (key in map.keys()) {
            val value = map.get(key)!!
            if (!enum.isKnown(value)) {
                map.set(key, enum.zero)
            }
        }

        return null
    }
}
