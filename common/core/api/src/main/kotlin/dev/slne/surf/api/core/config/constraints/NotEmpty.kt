package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Ensures that a collection, map, array, or string config value is not empty.
 *
 * This annotation enforces that the size or length of the annotated value is greater
 * than 0. If the validation fails, a `SerializationException` is thrown with a
 * descriptive error message.
 */
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