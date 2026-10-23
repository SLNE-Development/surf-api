package dev.slne.surf.nats.gradle.generator

import dev.slne.surf.nats.protocol.ProtocolFieldRules

internal object DefaultTypeMappings {
    private const val TYPES = "dev.slne.surf.nats.types"
    private const val PAPER_TYPES = "dev.slne.surf.nats.paper.types"

    val common: List<TypeMapping> = listOf(
        TypeMapping("google.protobuf.Timestamp", "java.time.Instant", "$TYPES.TimestampSerializer"),
        TypeMapping("google.protobuf.Duration", "kotlin.time.Duration", "$TYPES.DurationSerializer"),
        TypeMapping(ProtocolFieldRules.EMPTY_TYPE, "kotlin.Unit", "$TYPES.EmptySerializer"),
        TypeMapping(ProtocolFieldRules.UUID_TYPE, "java.util.UUID", "$TYPES.UuidSerializer"),
        TypeMapping("surf.types.v1.Component", "net.kyori.adventure.text.Component", "$TYPES.ComponentSerializer"),
        TypeMapping("surf.types.v1.Key", "net.kyori.adventure.key.Key", "$TYPES.KeySerializer"),
    )

    val paper: List<TypeMapping> = listOf(
        TypeMapping("surf.types.paper.v1.Location", "org.bukkit.Location", "$PAPER_TYPES.LocationSerializer"),
        TypeMapping("surf.types.paper.v1.ItemStack", "org.bukkit.inventory.ItemStack", "$PAPER_TYPES.ItemStackSerializer"),
    )

    /**
     * Returns [defaults] overridden by [overrides] with the same proto type, keyed by proto type.
     */
    fun resolve(defaults: List<TypeMapping>, overrides: List<TypeMapping>): Map<String, TypeMapping> {
        val result = LinkedHashMap<String, TypeMapping>()
        for (mapping in defaults + overrides) result[mapping.protoType] = mapping
        return result
    }
}
