package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage

/**
 * The rules of one field. [apply] returns the first violation in the field, or `null`.
 *
 * Steps with [validatesOutgoing] `false` only normalize incoming messages and are skipped for outgoing ones.
 */
internal sealed class FieldStep(val validatesOutgoing: Boolean) {

    abstract fun apply(message: ReflectMessage, direction: Direction): Violation?
}
