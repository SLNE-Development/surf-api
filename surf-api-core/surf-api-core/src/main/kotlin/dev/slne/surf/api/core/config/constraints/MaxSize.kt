package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

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