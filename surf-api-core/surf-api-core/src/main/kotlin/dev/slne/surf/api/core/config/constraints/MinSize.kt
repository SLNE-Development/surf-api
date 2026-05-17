package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Specifies a minimum size constraint for collections, maps, arrays, or strings.
 *
 * This annotation enforces that the size or length of the annotated value is at least
 * the specified minimum. If the validation fails, a `SerializationException` is thrown
 * with a descriptive error message.
 *
 * @property min The minimum required size or length for the annotated value.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinSize(val min: Int) {
    companion object {
        internal object Factory : Constraint.Factory<MinSize, Any?> {
            override fun make(data: MinSize, type: Type): Constraint<Any?> = { value ->
                val size = value.configSizeOrNull()
                if (size != null && size < data.min) {
                    throw SerializationException("Collection size is too small: $size, expected >= ${data.min}")
                }
            }
        }
    }
}