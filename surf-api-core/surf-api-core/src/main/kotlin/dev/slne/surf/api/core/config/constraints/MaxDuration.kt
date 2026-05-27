package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.ConfigDuration
import dev.slne.surf.api.core.config.type.DurationOrDisabled
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Annotation for constraining a `ConfigDuration`'s value to a maximum duration.
 *
 * This annotation ensures that the duration value of the annotated field does not exceed
 * the specified number of seconds. If the validation fails, a `SerializationException`
 * is thrown with a descriptive error message.
 *
 * @property seconds The maximum duration in seconds that the annotated field can have.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MaxDuration(val seconds: Long) {
    companion object {
        internal object Factory : Constraint.Factory<MaxDuration, ConfigDuration?> {
            override fun make(data: MaxDuration, type: Type): Constraint<ConfigDuration?> = { value ->
                if (value != null && value.value.inWholeSeconds > data.seconds) {
                    throw SerializationException("Duration is too long: ${value.value}, expected <= ${data.seconds}s")
                }
            }
        }

        internal object FactoryDurationOrDisabled : Constraint.Factory<MaxDuration, DurationOrDisabled?> {
            override fun make(data: MaxDuration, type: Type): Constraint<DurationOrDisabled?> = { durationOrDisabled ->
                val value = durationOrDisabled?.value

                if (value != null && value.inWholeSeconds > data.seconds) {
                    throw SerializationException("Duration is too long: ${value}, expected <= ${data.seconds}s")
                }
            }
        }
    }
}