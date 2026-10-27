package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.js.external.protobuf.DescFieldScalar
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage

internal class ScalarStep(
    private val field: DescFieldScalar,
    private val required: Boolean,
    private val rule: ScalarRule?,
) : FieldStep(validatesOutgoing = true) {

    override fun apply(message: ReflectMessage, direction: Direction): Violation? {
        if (!message.isSet(field)) {
            return if (required) Violation.missing(field.localName) else null
        }

        val problem = rule?.problem(message.get(field)) ?: return null
        return Violation.invalid(field.localName, problem)
    }
}
