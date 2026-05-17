package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class DisallowValues(vararg val values: String) {
    companion object {
        internal object Factory : Constraint.Factory<DisallowValues, Any?> {
            override fun make(data: DisallowValues, type: Type): Constraint<Any?> = { value ->
                if (value != null && data.values.any { it.equals(value.toString(), ignoreCase = true) }) {
                    throw SerializationException("Value '$value' is not allowed")
                }
            }
        }
    }
}