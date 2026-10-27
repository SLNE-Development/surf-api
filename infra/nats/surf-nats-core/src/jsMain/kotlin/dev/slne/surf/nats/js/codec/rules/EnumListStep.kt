package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.DescFieldEnumList
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage

internal class EnumListStep(
    private val field: DescFieldEnumList,
    private val enum: EnumPlan,
) : FieldStep(validatesOutgoing = false) {

    override fun apply(message: ReflectMessage, direction: Direction): Violation? {
        if (direction == Direction.OUTGOING) return null

        val list = message.get(field)
        for (index in 0 until list.size) {
            val value = list.get(index)!!
            if (!enum.isKnown(value)) {
                list.set(index, enum.zero)
            }
        }

        return null
    }
}
