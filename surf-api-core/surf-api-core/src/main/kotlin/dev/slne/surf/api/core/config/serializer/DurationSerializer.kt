package dev.slne.surf.api.core.config.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.math.BigDecimal
import java.util.function.Predicate
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Configurate scalar serializer for Kotlin [Duration] values.
 *
 * Supported configuration formats:
 * - `10s`
 * - `5m`
 * - `2h`
 * - `1d`
 *
 * Serialized durations are written back as seconds.
 */
internal object DurationSerializer : ScalarSerializer.Annotated<Duration>(Duration::class.java) {
    private val DURATION_PATTERN = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*([dhms])\s*$""", RegexOption.IGNORE_CASE)

    /**
     * Parses a duration string into a Kotlin [Duration].
     */
    override fun deserialize(type: AnnotatedType, obj: Any): Duration {
        val value = obj.toString()
        val match = DURATION_PATTERN.matchEntire(value)
            ?: throw SerializationException(Duration::class.java, "$obj($type) is not a duration")

        val amount = match.groupValues[1].toDoubleOrNull()
            ?: throw SerializationException(Duration::class.java, "$obj($type) is not a duration")

        val unit = when (match.groupValues[2].lowercase()) {
            "d" -> DurationUnit.DAYS
            "h" -> DurationUnit.HOURS
            "m" -> DurationUnit.MINUTES
            "s" -> DurationUnit.SECONDS
            else -> throw SerializationException(Duration::class.java, "$obj($type) is not a duration")
        }

        return amount.toDuration(unit)
    }

    /**
     * Serializes a finite Kotlin [Duration] as a seconds-based duration string.
     *
     * @throws SerializationException if [item] is infinite.
     */
    public override fun serialize(type: AnnotatedType, item: Duration, typeSupported: Predicate<Class<*>>): Any {
        if (item.isInfinite()) {
            throw SerializationException(Duration::class.java, "$item($type) is infinite and cannot be serialized")
        }

        return "${formatNumber(item.toDouble(DurationUnit.SECONDS))}s"
    }

    private fun formatNumber(value: Double): String {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }
}