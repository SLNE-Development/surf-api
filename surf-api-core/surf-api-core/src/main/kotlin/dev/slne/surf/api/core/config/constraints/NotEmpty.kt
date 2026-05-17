package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotEmpty {
    companion object {
        internal object Factory : Constraint.Factory<NotEmpty, Any?> {
            override fun make(data: NotEmpty, type: Type): Constraint<Any?> = { value ->
                val size = value.configSizeOrNull()
                if (size != null && size == 0) {
                    throw SerializationException("Value must not be empty")
                }
            }
        }
    }
}