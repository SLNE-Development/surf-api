package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage

/**
 * The rules of one field; [apply] returns the first violation in the field or `null`.
 */
internal sealed class FieldStep(val validatesOutgoing: Boolean) {

    abstract fun apply(message: ReflectMessage, direction: Direction): Violation?
}
