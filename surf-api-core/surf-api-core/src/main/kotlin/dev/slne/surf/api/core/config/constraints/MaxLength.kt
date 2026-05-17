package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MaxLength(val max: Int) {
    companion object {
        internal object Factory : Constraint.Factory<MaxLength, String?> {
            override fun make(data: MaxLength, type: Type): Constraint<String?> = { value ->
                if (value != null && value.length > data.max) {
                    throw SerializationException("String is too long: ${value.length}, expected <= ${data.max}")
                }
            }
        }
    }
}