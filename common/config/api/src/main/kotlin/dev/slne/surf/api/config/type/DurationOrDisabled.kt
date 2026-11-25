package dev.slne.surf.api.config.type

import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate
import kotlin.time.Duration

/**
 * Represents a duration configuration value that can either contain a Kotlin [Duration],
 * or be explicitly disabled.
 *
 * The serialized configuration values are:
 * - duration strings such as `10s`, `5m`, `2h`, or `1d`
 * - `disabled`
 *
 * Usage:
 * ```kotlin
 * import kotlin.time.Duration.Companion.seconds
 *
 * @ConfigSerializable
 * data class TimeoutConfig(
 *     val timeout: DurationOrDisabled = DurationOrDisabled.DISABLED
 * )
 *
 * val timeout = config.timeout or 30.seconds
 *
 * if (config.timeout.isDisabled()) {
 *     // Disable timeout handling
 * }
 * ```
 */
@ConsistentCopyVisibility
data class DurationOrDisabled private constructor(val value: Duration?) {

    /**
     * Returns the configured duration, or [other] if this value is disabled.
     */
    infix fun or(other: Duration): Duration = value ?: other

    /**
     * Returns `true` if this value is configured as `disabled`.
     */
    fun isDisabled(): Boolean = value == null

    companion object {
        private const val DISABLED_VALUE = "disabled"

        /**
         * Represents the `disabled` configuration value.
         */
        @JvmField
        val DISABLED = DurationOrDisabled(null)
    }

    /**
     * Configurate serializer for [DurationOrDisabled].
     */
    @SurfConfigSerializer
    internal class Serializer :
        ScalarSerializer.Annotated<DurationOrDisabled>(DurationOrDisabled::class.java) {

        override fun deserialize(type: AnnotatedType, obj: Any): DurationOrDisabled {
            if (obj is String) {
                if (obj.equals(DISABLED_VALUE, ignoreCase = true)) {
                    return DISABLED
                }

                return try {
                    DurationOrDisabled(
                        ConfigDuration.parse(obj).asDuration(),
                    )
                } catch (e: Exception) {
                    throw SerializationException(
                        DurationOrDisabled::class.java,
                        "$obj($type) is not a duration or '$DISABLED_VALUE'",
                        e,
                    )
                }
            }

            throw SerializationException(
                DurationOrDisabled::class.java,
                "$obj($type) is not a duration or '$DISABLED_VALUE'",
            )
        }

        override fun serialize(
            type: AnnotatedType,
            item: DurationOrDisabled,
            typeSupported: Predicate<Class<*>>,
        ): Any {
            val duration = item.value ?: return DISABLED_VALUE

            return ConfigDuration(duration).rawValue
        }
    }
}