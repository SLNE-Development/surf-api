package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import java.math.BigDecimal
import java.math.BigInteger
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.*
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.types.legacy.BigDecimalSerializer as LegacyBigDecimalSerializer
import dev.slne.surf.nats.types.legacy.BigIntegerSerializer as LegacyBigIntegerSerializer
import dev.slne.surf.nats.types.legacy.ComponentSerializer as LegacyComponentSerializer
import dev.slne.surf.nats.types.legacy.DurationSerializer as LegacyDurationSerializer
import dev.slne.surf.nats.types.legacy.EmptySerializer as LegacyEmptySerializer
import dev.slne.surf.nats.types.legacy.InetSocketAddressSerializer as LegacyInetSocketAddressSerializer
import dev.slne.surf.nats.types.legacy.IpAddressSerializer as LegacyIpAddressSerializer
import dev.slne.surf.nats.types.legacy.KeySerializer as LegacyKeySerializer
import dev.slne.surf.nats.types.legacy.LocalDateSerializer as LegacyLocalDateSerializer
import dev.slne.surf.nats.types.legacy.LocalDateTimeSerializer as LegacyLocalDateTimeSerializer
import dev.slne.surf.nats.types.legacy.LocalTimeSerializer as LegacyLocalTimeSerializer
import dev.slne.surf.nats.types.legacy.OffsetDateTimeSerializer as LegacyOffsetDateTimeSerializer
import dev.slne.surf.nats.types.legacy.ShadowColorSerializer as LegacyShadowColorSerializer
import dev.slne.surf.nats.types.legacy.SoundSerializer as LegacySoundSerializer
import dev.slne.surf.nats.types.legacy.TextColorSerializer as LegacyTextColorSerializer
import dev.slne.surf.nats.types.legacy.TimestampSerializer as LegacyTimestampSerializer
import dev.slne.surf.nats.types.legacy.TitleSerializer as LegacyTitleSerializer
import dev.slne.surf.nats.types.legacy.UuidSerializer as LegacyUuidSerializer
import dev.slne.surf.nats.types.legacy.ZonedDateTimeSerializer as LegacyZonedDateTimeSerializer

/**
 * Pins the wire format of every serializer to the implementation it replaced.
 *
 * The `legacy` package holds verbatim copies of the serializers before they were rebuilt on top of
 * surf-api-serialization. Each test encodes a corpus with both and decodes the legacy bytes with
 * the current serializer.
 */
class LegacyWireCompatibilityTest {

    @Test
    fun uuid() = assertSameWire(
        LegacyUuidSerializer,
        UuidSerializer,
        UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"),
        UUID(0, 0),
        UUID(-1, -1),
        UUID.nameUUIDFromBytes(byteArrayOf(1, 2, 3)),
    )

    @Test
    fun component() = assertSameWire(
        LegacyComponentSerializer,
        ComponentSerializer,
        Component.empty(),
        Component.text("a"),
        Component.text().append(Component.text("a")).build(),
        Component.text("red", NamedTextColor.RED, TextDecoration.BOLD),
        Component.translatable("block.minecraft.stone", Component.text("arg")),
        Component.text("äöü ☃ \"quoted\""),
    )

    @Test
    fun key() = assertSameWire(
        LegacyKeySerializer,
        KeySerializer,
        Key.key("stone"),
        Key.key("surf", "path/with.dots-and_underscores"),
    )

    @Test
    fun textColor() = assertSameWire(
        LegacyTextColorSerializer,
        TextColorSerializer,
        *NamedTextColor.NAMES.values().toTypedArray(),
        TextColor.color(0),
        TextColor.color(0xFFFFFF),
        TextColor.color(0xFF6B6B),
    )

    @Test
    fun shadowColor() = assertSameWire(
        LegacyShadowColorSerializer,
        ShadowColorSerializer,
        ShadowColor.shadowColor(0),
        ShadowColor.shadowColor(-1),
        ShadowColor.shadowColor(0x7F123456),
        ShadowColor.shadowColor(0x80123456.toInt()),
    )

    @Test
    fun ipAddress() = assertSameWire(
        LegacyIpAddressSerializer,
        IpAddressSerializer,
        InetAddress.ofLiteral("127.0.0.1"),
        InetAddress.ofLiteral("0.0.0.0"),
        InetAddress.ofLiteral("255.255.255.255"),
        InetAddress.ofLiteral("::1"),
        InetAddress.ofLiteral("2001:db8::1"),
        InetAddress.getByAddress("localhost", byteArrayOf(127, 0, 0, 1)),
    )

    @Test
    fun bigInteger() = assertSameWire(
        LegacyBigIntegerSerializer,
        BigIntegerSerializer,
        BigInteger.ZERO,
        BigInteger.ONE.negate(),
        BigInteger("123456789012345678901234567890"),
        BigInteger("-98765432109876543210"),
    )

    @Test
    fun bigDecimal() = assertSameWire(
        LegacyBigDecimalSerializer,
        BigDecimalSerializer,
        BigDecimal.ZERO,
        BigDecimal("12.50"),
        BigDecimal("12.5"),
        BigDecimal("1E+3"),
        BigDecimal("-0.001"),
        BigDecimal("123456789012345678901234567890.0123456789"),
    )

    @Test
    fun duration() = assertSameWire(
        LegacyDurationSerializer,
        DurationSerializer,
        kotlin.time.Duration.ZERO,
        1.seconds,
        (-1.5).seconds,
        250.milliseconds,
        1.nanoseconds,
        (315_576_000_000L).seconds,
    )

    @Test
    fun timestamp() = assertSameWire(
        LegacyTimestampSerializer,
        TimestampSerializer,
        Instant.EPOCH,
        Instant.ofEpochSecond(1_700_000_000, 123_456_789),
        Instant.ofEpochSecond(-1, 1),
    )

    @Test
    fun empty() = assertSameWire(LegacyEmptySerializer, EmptySerializer, Unit)

    @Test
    fun localDate() = assertSameWire(
        LegacyLocalDateSerializer,
        LocalDateSerializer,
        LocalDate.of(2026, 11, 20),
        LocalDate.of(0, 1, 1),
        LocalDate.of(-5, 2, 28),
    )

    @Test
    fun localTime() = assertSameWire(
        LegacyLocalTimeSerializer,
        LocalTimeSerializer,
        LocalTime.MIDNIGHT,
        LocalTime.of(23, 59, 59, 999_999_999),
        LocalTime.of(12, 0),
    )

    @Test
    fun localDateTime() = assertSameWire(
        LegacyLocalDateTimeSerializer,
        LocalDateTimeSerializer,
        LocalDateTime.of(2026, 11, 20, 0, 0),
        LocalDateTime.of(1999, 12, 31, 23, 59, 59, 1),
    )

    @Test
    fun offsetDateTime() = assertSameWire(
        LegacyOffsetDateTimeSerializer,
        OffsetDateTimeSerializer,
        OffsetDateTime.of(2026, 11, 20, 10, 30, 0, 5, ZoneOffset.ofHours(2)),
        OffsetDateTime.of(1969, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC),
    )

    @Test
    fun zonedDateTime() = assertSameWire(
        LegacyZonedDateTimeSerializer,
        ZonedDateTimeSerializer,
        ZonedDateTime.of(2026, 3, 29, 2, 30, 0, 0, ZoneId.of("Europe/Berlin")),
        ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.ofHours(-5)),
    )

    @Test
    fun inetSocketAddress() = assertSameWire(
        LegacyInetSocketAddressSerializer,
        InetSocketAddressSerializer,
        InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), 25565),
        InetSocketAddress(InetAddress.getByAddress("host", byteArrayOf(10, 0, 0, 1)), 0),
        InetSocketAddress.createUnresolved("example.invalid", 65535),
    )

    @Test
    fun sound() = assertSameWire(
        LegacySoundSerializer,
        SoundSerializer,
        Sound.sound(Key.key("entity.player.levelup"), Sound.Source.PLAYER, 1f, 0.5f),
        Sound.sound().type(Key.key("surf", "ding")).source(Sound.Source.UI).seed(42).build(),
        Sound.sound(Key.key("ambient.cave"), Sound.Source.MASTER, 0f, 0f),
    )

    @Test
    fun title() = assertSameWire(
        LegacyTitleSerializer,
        TitleSerializer,
        Title.title(Component.text("a"), Component.empty(), null),
        Title.title(
            Component.text("title", NamedTextColor.GOLD),
            Component.text("subtitle"),
            Title.Times.times(
                java.time.Duration.ofMillis(500),
                java.time.Duration.ofSeconds(3),
                java.time.Duration.ZERO,
            ),
        ),
    )

    private fun <T> assertSameWire(
        legacy: KSerializer<T>,
        current: KSerializer<T>,
        vararg values: T,
    ) {
        for (value in values) {
            val legacyHex = encodeHex(legacy, value)

            assertEquals(legacyHex, encodeHex(current, value), "encoding of $value")
            assertEquals(
                legacyHex,
                encodeHex(legacy, decodeHex(current, legacyHex)),
                "decoding of $value",
            )
        }
    }
}
