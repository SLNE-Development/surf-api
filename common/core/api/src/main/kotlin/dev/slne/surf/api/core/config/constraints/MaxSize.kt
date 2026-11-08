package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Specifies a maximum size constraint for collections, maps, arrays, or strings.
 *
 * This annotation enforces that the size or length of the annotated value does not exceed
 * the specified maximum. If the validation fails, a `SerializationException` is thrown
 * with a descriptive error message.
 *
 * @property max The maximum allowed size or length for the annotated value.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MaxSize(val max: Int) {
    companion object {
        internal object Factory : Constraint.Factory<MaxSize, Any?> {
            override fun make(data: MaxSize, type: Type): Constraint<Any?> = { value ->
                val size = value.configSizeOrNull()
                if (size != null && size > data.max) {
                    throw SerializationException("Collection size is too large: $size, expected <= ${data.max}")
                }
            }
        }
    }
}