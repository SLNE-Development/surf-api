package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.ConfigDuration
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

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
    }
}