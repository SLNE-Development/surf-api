package dev.slne.surf.api.core.config.type

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.math.BigDecimal
import java.util.function.Predicate
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class ConfigDuration(
    val value: Duration,
    val rawValue: String = format(value)
) {
    fun asDuration(): Duration = value
    fun inWholeSeconds(): Long = value.inWholeSeconds

    companion object {
        private val DURATION_PATTERN =
            Regex("""^\s*(-?\d+(?:\.\d+)?)\s*([dhms])\s*$""", RegexOption.IGNORE_CASE)

        fun parse(input: String): ConfigDuration {
            val cleaned = input.trim()
            val match = DURATION_PATTERN.matchEntire(cleaned)
                ?: throw SerializationException(ConfigDuration::class.java, "$input is not a duration")

            val amount = match.groupValues[1].toDoubleOrNull()
                ?: throw SerializationException(ConfigDuration::class.java, "$input is not a duration")

            val unit = when (match.groupValues[2].lowercase()) {
                "d" -> DurationUnit.DAYS
                "h" -> DurationUnit.HOURS
                "m" -> DurationUnit.MINUTES
                "s" -> DurationUnit.SECONDS
                else -> throw SerializationException(ConfigDuration::class.java, "$input is not a duration")
            }

            return ConfigDuration(amount.toDuration(unit), cleaned)
        }

        fun format(duration: Duration): String {
            if (duration.isInfinite()) {
                throw SerializationException(
                    ConfigDuration::class.java,
                    "$duration is infinite and cannot be serialized"
                )
            }

            return when (duration) {
                duration.inWholeDays.toDuration(DurationUnit.DAYS) -> "${duration.inWholeDays}d"
                duration.inWholeHours.toDuration(DurationUnit.HOURS) -> "${duration.inWholeHours}h"
                duration.inWholeMinutes.toDuration(DurationUnit.MINUTES) -> "${duration.inWholeMinutes}m"
                else -> "${duration.toDouble(DurationUnit.SECONDS).formatNumber()}s"
            }
        }
    }

    internal object Serializer : ScalarSerializer.Annotated<ConfigDuration>(ConfigDuration::class.java) {
        override fun deserialize(type: AnnotatedType, obj: Any): ConfigDuration {
            return parse(obj.toString())
        }

        public override fun serialize(
            type: AnnotatedType,
            item: ConfigDuration,
            typeSupported: Predicate<Class<*>>
        ): Any {
            return item.rawValue
        }
    }
}

private fun Double.formatNumber(): String {
    return BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
}