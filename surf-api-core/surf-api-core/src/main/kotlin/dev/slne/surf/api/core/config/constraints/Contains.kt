package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Contains(val value: String) {
    companion object {
        internal object Factory : Constraint.Factory<Contains, String?> {
            override fun make(data: Contains, type: Type): Constraint<String?> = { value ->
                if (value != null && !value.contains(data.value)) {
                    throw SerializationException("String must contain '${data.value}'")
                }
            }
        }
    }
}