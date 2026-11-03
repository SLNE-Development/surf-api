package dev.slne.surf.nats.gradle.generator

import com.squareup.wire.schema.ProtoType
import dev.slne.surf.nats.protocol.ProtocolFieldRules

internal object DefaultTypeMappings {
    private const val TYPES = "dev.slne.surf.nats.types"
    private const val PAPER_TYPES = "dev.slne.surf.nats.paper.types"

    val common: List<TypeMapping> = listOf(
        TypeMapping(
            ProtoType.TIMESTAMP,
            "java.time.Instant",
            "$TYPES.TimestampSerializer",
        ),
        TypeMapping(
            ProtoType.DURATION,
            "kotlin.time.Duration",
            "$TYPES.DurationSerializer",
        ),
        TypeMapping(
            ProtoType.EMPTY,
            "kotlin.Unit",
            "$TYPES.EmptySerializer",
        ),
        TypeMapping(
            ProtocolFieldRules.UUID_TYPE,
            "java.util.UUID",
            "$TYPES.UuidSerializer",
        ),
        TypeMapping(
            ProtocolFieldRules.COMPONENT_TYPE,
            "net.kyori.adventure.text.Component",
            "$TYPES.ComponentSerializer",
        ),
        TypeMapping(
            ProtocolFieldRules.KEY_TYPE,
            "net.kyori.adventure.key.Key",
            "$TYPES.KeySerializer",
        ),
    )

    val paper: List<TypeMapping> = listOf(
        TypeMapping(
            ProtocolFieldRules.LOCATION_TYPE,
            "org.bukkit.Location",
            "$PAPER_TYPES.LocationSerializer",
        ),
        TypeMapping(
            ProtocolFieldRules.ITEM_STACK_TYPE,
            "org.bukkit.inventory.ItemStack",
            "$PAPER_TYPES.ItemStackSerializer",
        ),
    )
}