package dev.slne.surf.nats.interop

import dev.slne.surf.nats.TEST_UUID
import dev.slne.surf.nats.test.proto.v1.Color
import dev.slne.surf.nats.test.proto.v1.ColorHolder
import dev.slne.surf.nats.test.proto.v1.InteropDefaults
import dev.slne.surf.nats.test.proto.v1.InteropEmpty
import dev.slne.surf.nats.test.proto.v1.InteropEvolved
import dev.slne.surf.nats.test.proto.v1.InteropEvolvedV2
import dev.slne.surf.nats.test.proto.v1.InteropMaps
import dev.slne.surf.nats.test.proto.v1.InteropNullables
import dev.slne.surf.nats.test.proto.v1.InteropOneof
import dev.slne.surf.nats.test.proto.v1.InteropPacked
import dev.slne.surf.nats.test.proto.v1.InteropRequired
import dev.slne.surf.nats.test.proto.v1.InteropScalars
import dev.slne.surf.nats.test.proto.v1.InteropTypes
import dev.slne.surf.nats.test.proto.v1.WideColor
import dev.slne.surf.nats.test.proto.v1.WideColorHolder
import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

/**
 * The wire vectors of spec 12.7; `src/jsTest/interop/vectors.mjs` holds the JS expectations under the same names.
 */
internal object InteropVectors {
    val INSTANT: Instant = Instant.ofEpochSecond(1_700_000_000, 123_456_789)
    val NEGATIVE_DURATION: Duration = (-1_500).milliseconds

    private val zeroScalars = InteropScalars(0, 0L, 0L, 0, 0L, 0, 0.0, false)

    // Kotlin decodes WIDE_COLOR_PURPLE (4) as ColorHolder to UNSPECIFIED in every position (spec 12.9 E).
    private val unknownColorsDecoded = ColorHolder(
        color = Color.UNSPECIFIED,
        maybeColor = Color.UNSPECIFIED,
        colors = listOf(Color.UNSPECIFIED, Color.RED),
        colorsByName = mapOf("a" to Color.UNSPECIFIED, "b" to Color.GREEN),
    )

    val unknownColorsWide = WideColorHolder(
        color = WideColor.PURPLE,
        maybeColor = WideColor.PURPLE,
        colors = listOf(WideColor.PURPLE, WideColor.RED),
        colorsByName = mapOf("a" to WideColor.PURPLE, "b" to WideColor.GREEN),
    )

    val all: List<InteropVector> = listOf(
        InteropVector(
            "scalars-extremes", InteropScalars.serializer(),
            InteropScalars(
                unsignedSmall = -1,
                unsignedLarge = -1L,
                signedLarge = Long.MIN_VALUE,
                small = Int.MIN_VALUE,
                large = Long.MIN_VALUE,
                fixedSmall = -1,
                ratio = -0.25,
                flag = true,
            ),
        ),
        InteropVector(
            "scalars-2^31", InteropScalars.serializer(),
            InteropScalars(
                unsignedSmall = Int.MIN_VALUE,
                unsignedLarge = Long.MIN_VALUE,
                signedLarge = Long.MAX_VALUE,
                small = Int.MAX_VALUE,
                large = Long.MAX_VALUE,
                fixedSmall = Int.MIN_VALUE,
                ratio = 1e300,
                flag = false,
            ),
        ),
        InteropVector("scalars-zero", InteropScalars.serializer(), zeroScalars),
        InteropVector(
            "maps", InteropMaps.serializer(),
            InteropMaps(
                signedByName = mapOf("min" to Long.MIN_VALUE, "minus-one" to -1L, "zero" to 0L, "max" to Long.MAX_VALUE),
                namesByFixed = mapOf(0 to "zero", 1 to "one", Int.MIN_VALUE to "2^31", -1 to "2^32-1"),
            ),
        ),
        InteropVector("maps-empty", InteropMaps.serializer(), InteropMaps()),
        InteropVector(
            "packed", InteropPacked.serializer(),
            InteropPacked(
                packedValues = listOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE, 300),
                unpackedValues = listOf(7, -7, 0),
            ),
        ),
        InteropVector("packed-empty", InteropPacked.serializer(), InteropPacked()),
        InteropVector(
            "colors-known", ColorHolder.serializer(),
            ColorHolder(
                color = Color.RED,
                maybeColor = Color.BLUE,
                colors = listOf(Color.GREEN, Color.UNSPECIFIED, Color.BLUE),
                colorsByName = mapOf("a" to Color.RED, "zero" to Color.UNSPECIFIED),
            ),
        ),
        InteropVector(
            "colors-unknown-from-kotlin", WideColorHolder.serializer(), unknownColorsWide,
            echo = unknownColorsDecoded,
            raw = true,
        ),
        InteropVector("colors-unknown-from-js", ColorHolder.serializer(), unknownColorsDecoded),
        InteropVector("nullables-null", InteropNullables.serializer(), InteropNullables()),
        InteropVector(
            "nullables-zero", InteropNullables.serializer(),
            InteropNullables(
                text = "",
                number = 0,
                data = byteArrayOf(),
                scalars = zeroScalars,
                uuid = UUID(0, 0),
                color = Color.UNSPECIFIED,
            ),
        ),
        InteropVector("defaults-elided", InteropDefaults.serializer(), InteropDefaults()),
        InteropVector(
            "defaults-explicit", InteropDefaults.serializer(),
            InteropDefaults(
                number = 0,
                text = "",
                flag = false,
                ratio = 0.0,
                color = Color.RED,
                data = byteArrayOf(),
                unsignedSmall = 0,
            ),
        ),
        InteropVector("defaults-sent-by-js", InteropDefaults.serializer(), InteropDefaults()),
        InteropVector("required", InteropRequired.serializer(), InteropRequired(name = "steve")),
        InteropVector("required-empty", InteropRequired.serializer(), InteropRequired(name = "", count = 0)),
        // A sender without InteropRequired.name: the empty message encodes to 0 bytes.
        InteropVector("required-missing", InteropEmpty.serializer(), InteropEmpty(), echo = null, raw = true),
        InteropVector("types", InteropTypes.serializer(), InteropTypes(TEST_UUID, INSTANT, NEGATIVE_DURATION)),
        InteropVector(
            "types-epoch", InteropTypes.serializer(),
            InteropTypes(UUID(Long.MIN_VALUE, 1L), Instant.EPOCH, Duration.ZERO),
        ),
        InteropVector(
            "types-negative-subsecond", InteropTypes.serializer(),
            InteropTypes(TEST_UUID, Instant.ofEpochSecond(-1, 999_999_999), (-1).nanoseconds),
        ),
        InteropVector("empty", InteropEmpty.serializer(), InteropEmpty()),
        InteropVector("oneof-unset", InteropOneof.serializer(), InteropOneof()),
        InteropVector("oneof-text", InteropOneof.serializer(), InteropOneof(InteropOneof.Value.Text(""))),
        InteropVector(
            "oneof-number", InteropOneof.serializer(),
            InteropOneof(InteropOneof.Value.Number(Long.MIN_VALUE)),
        ),
        InteropVector(
            "oneof-empty", InteropOneof.serializer(),
            InteropOneof(InteropOneof.Value.Empty(InteropEmpty())),
        ),
        // Schema evolution: a newer Kotlin sender for an older JS reader, which keeps the new fields as unknown
        // fields; its echo shows that the older Kotlin reader skips them.
        InteropVector(
            "evolved-new-to-old", InteropEvolvedV2.serializer(),
            InteropEvolvedV2(name = "steve", level = 3, nickname = "st", rank = 9),
            echo = InteropEvolved(name = "steve", level = 3),
            raw = true,
        ),
        // An older Kotlin sender for a newer JS reader; both newer readers restore null and the [default].
        InteropVector(
            "evolved-old-to-new", InteropEvolved.serializer(),
            InteropEvolved(name = "alex", level = 0),
            echo = InteropEvolvedV2(name = "alex", level = 0, nickname = null, rank = 5),
            raw = true,
        ),
    )
}
