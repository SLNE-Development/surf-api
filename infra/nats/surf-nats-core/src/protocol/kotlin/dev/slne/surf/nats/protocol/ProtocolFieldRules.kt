package dev.slne.surf.nats.protocol

import com.squareup.wire.schema.ProtoType

internal object ProtocolFieldRules {
    const val WELL_KNOWN_PREFIX = "google.protobuf."
    val UUID_TYPE = ProtoType.get("surf.types.v1.Uuid")
    const val UUID_BYTES = 16

    val COMPONENT_TYPE = ProtoType.get("surf.types.v1.Component")
    val KEY_TYPE = ProtoType.get("surf.types.v1.Key")

    val LOCATION_TYPE = ProtoType.get("surf.types.paper.v1.Location")
    val ITEM_STACK_TYPE = ProtoType.get("surf.types.paper.v1.ItemStack")

    /**
     * Returns whether the Kotlin codec rejects a message without the field.
     *
     * Fields without explicit presence (repeated, map) and oneof members decode to empty or unset values.
     */
    fun isRequired(
        explicitPresence: Boolean,
        oneofMember: Boolean,
        nullable: Boolean,
        hasDefault: Boolean
    ): Boolean {
        return explicitPresence && !oneofMember && !nullable && !hasDefault
    }
}
