package dev.slne.surf.nats.js.codec.rules

import dev.slne.surf.nats.protocol.ProtocolFieldRules
import js.typedarrays.Uint8Array

/**
 * Value check of the set scalar fields of one message type; returns the problem or `null`.
 */
internal fun interface ScalarRule {
    fun problem(value: Any): String?

    companion object {
        private val UUID = ScalarRule { value ->
            val bytes = value as? Uint8Array<*>
                ?: return@ScalarRule "must be a byte array"

            if (bytes.length == ProtocolFieldRules.UUID_BYTES) {
                null
            } else {
                "must be exactly ${ProtocolFieldRules.UUID_BYTES} bytes, was ${bytes.length}"
            }
        }

        fun forType(typeName: String): ScalarRule? {
            return if (typeName == ProtocolFieldRules.UUID_TYPE) UUID else null
        }
    }
}
