package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.DescFieldEnum
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage

internal class EnumStep(
    private val field: DescFieldEnum,
    private val enum: EnumPlan,
    private val required: Boolean,
) : FieldStep(validatesOutgoing = required) {

    override fun apply(message: ReflectMessage, direction: Direction): Violation? {
        if (direction == Direction.INCOMING) normalize(message)

        return if (required && !message.isSet(field)) {
            Violation.missing(field.localName)
        } else {
            null
        }
    }

    private fun normalize(message: ReflectMessage) {
        if (message.isSet(field)) {
            replaceUnknownOpenEnumValue(message)
        } else {
            restoreClosedEnumValueFromUnknownFields(message)
        }
    }

    private fun replaceUnknownOpenEnumValue(message: ReflectMessage) {
        val value = message.get(field)
        if (!enum.isKnown(value)) {
            message.set(field, enum.zero)
        }
    }

    private fun restoreClosedEnumValueFromUnknownFields(message: ReflectMessage) {
        val unknownFields = message.getUnknown() ?: return
        val matchingCount = unknownFields.count { it.no == field.number }
        if (matchingCount == 0) return

        val oneof = field.oneof
        if (oneof == null || message.oneofCase(oneof) == null) {
            message.set(field, enum.zero)
        }

        val remainingCount = unknownFields.size - matchingCount
        if (remainingCount == 0) {
            message.setUnknown(undefined)
            return
        }

        val remaining = unknownFields.copyOfRange(0, remainingCount)
        var index = 0

        for (unknown in unknownFields) {
            if (unknown.no != field.number) {
                remaining[index++] = unknown
            }
        }

        message.setUnknown(remaining)
    }
}
