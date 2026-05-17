package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import kotlin.time.Duration

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinDuration(val seconds: Long) {
    companion object {
        internal object Factory : Constraint.Factory<MinDuration, Duration?> {
            override fun make(data: MinDuration, type: Type): Constraint<Duration?> = { value ->
                if (value != null && value.inWholeSeconds < data.seconds) {
                    throw SerializationException("Duration is too short: $value, expected >= ${data.seconds}s")
                }
            }
        }
    }
}