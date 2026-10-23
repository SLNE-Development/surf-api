package dev.slne.surf.nats.protocol

internal object ProtocolFieldRules {
    const val WELL_KNOWN_PREFIX = "google.protobuf."
    const val EMPTY_TYPE = "google.protobuf.Empty"
    const val UUID_TYPE = "surf.types.v1.Uuid"
    const val UUID_BYTES = 16

    /**
     * Returns whether the Kotlin codec rejects a message without the field.
     *
     * Fields without explicit presence (repeated, map) and oneof members decode to empty or unset values.
     */
    fun isRequired(explicitPresence: Boolean, oneofMember: Boolean, nullable: Boolean, hasDefault: Boolean): Boolean {
        return explicitPresence && !oneofMember && !nullable && !hasDefault
    }
}
