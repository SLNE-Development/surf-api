package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.ConfigDuration
import dev.slne.surf.api.core.config.type.DurationOrDisabled
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Specifies a minimum duration constraint for configuration values annotated with this annotation.
 *
 * This annotation ensures that durations provided in the configuration meet or exceed
 * the specified minimum value in seconds. If the validation fails, a `SerializationException`
 * is thrown with a descriptive error message.
 *
 * @property seconds The minimum allowed duration in seconds.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinDuration(val seconds: Long) {
    companion object {
        internal object Factory : Constraint.Factory<MinDuration, ConfigDuration?> {
            override fun make(data: MinDuration, type: Type): Constraint<ConfigDuration?> = { value ->
                if (value != null && value.value.inWholeSeconds < data.seconds) {
                    throw SerializationException("Duration is too short: ${value.value}, expected >= ${data.seconds}s")
                }
            }
        }

        internal object FactoryDurationOrDisabled : Constraint.Factory<MinDuration, DurationOrDisabled?> {
            override fun make(data: MinDuration, type: Type): Constraint<DurationOrDisabled?> = { durationOrDisabled ->
                val value = durationOrDisabled?.value

                if (value != null && value.inWholeSeconds < data.seconds) {
                    throw SerializationException("Duration is too short: ${value}, expected >= ${data.seconds}s")
                }
            }
        }
    }
}