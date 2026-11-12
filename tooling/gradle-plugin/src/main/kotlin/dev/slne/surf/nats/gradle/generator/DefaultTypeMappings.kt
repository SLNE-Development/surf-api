package dev.slne.surf.nats.gradle.generator

import com.squareup.wire.schema.ProtoType
import dev.slne.surf.nats.protocol.ProtocolFieldRules

/**
 * The type mappings the plugin applies without `mapType`.
 *
 * The serializers live in `surf-api-nats-types-common` and `surf-api-nats-types-paper`. The `surf.types` protos are
 * bundled with the plugin. Explicit `mapType` calls override these mappings.
 */
internal object DefaultTypeMappings {
    private const val TYPES = "dev.slne.surf.nats.types"
    private const val PAPER_TYPES = "dev.slne.surf.nats.paper.types"

    /**
     * Mappings for every project: well-known types, UUID, Adventure, `java.time`, `java.math` and `java.net`.
     */
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
            ProtoType.get(ProtocolFieldRules.UUID_TYPE),
            "java.util.UUID",
            "$TYPES.UuidSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.Component"),
            "net.kyori.adventure.text.Component",
            "$TYPES.ComponentSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.Key"),
            "net.kyori.adventure.key.Key",
            "$TYPES.KeySerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.TextColor"),
            "net.kyori.adventure.text.format.TextColor",
            "$TYPES.TextColorSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.ShadowColor"),
            "net.kyori.adventure.text.format.ShadowColor",
            "$TYPES.ShadowColorSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.Sound"),
            "net.kyori.adventure.sound.Sound",
            "$TYPES.SoundSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.Title"),
            "net.kyori.adventure.title.Title",
            "$TYPES.TitleSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.OffsetDateTime"),
            "java.time.OffsetDateTime",
            "$TYPES.OffsetDateTimeSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.ZonedDateTime"),
            "java.time.ZonedDateTime",
            "$TYPES.ZonedDateTimeSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.LocalDate"),
            "java.time.LocalDate",
            "$TYPES.LocalDateSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.LocalTime"),
            "java.time.LocalTime",
            "$TYPES.LocalTimeSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.LocalDateTime"),
            "java.time.LocalDateTime",
            "$TYPES.LocalDateTimeSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.BigDecimal"),
            "java.math.BigDecimal",
            "$TYPES.BigDecimalSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.BigInteger"),
            "java.math.BigInteger",
            "$TYPES.BigIntegerSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.IpAddress"),
            "java.net.InetAddress",
            "$TYPES.IpAddressSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.v1.InetSocketAddress"),
            "java.net.InetSocketAddress",
            "$TYPES.InetSocketAddressSerializer",
        ),
    )

    /**
     * Additional mappings for projects with the surf-api `paper-plugin` or `paper-raw` plugin.
     */
    val paper: List<TypeMapping> = listOf(
        TypeMapping(
            ProtoType.get("surf.types.paper.v1.Location"),
            "org.bukkit.Location",
            "$PAPER_TYPES.LocationSerializer",
        ),
        TypeMapping(
            ProtoType.get("surf.types.paper.v1.ItemStack"),
            "org.bukkit.inventory.ItemStack",
            "$PAPER_TYPES.ItemStackSerializer",
        ),
    )
}