package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class StartsWith(val prefix: String) {
    companion object {
        internal object Factory : Constraint.Factory<StartsWith, String?> {
            override fun make(data: StartsWith, type: Type): Constraint<String?> = { value ->
                if (value != null && !value.startsWith(data.prefix)) {
                    throw SerializationException("String must start with '${data.prefix}'")
                }
            }
        }
    }
}
