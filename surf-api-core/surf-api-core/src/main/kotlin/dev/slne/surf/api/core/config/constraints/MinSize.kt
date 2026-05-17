package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

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