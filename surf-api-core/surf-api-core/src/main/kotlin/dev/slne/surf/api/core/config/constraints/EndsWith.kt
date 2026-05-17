package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class EndsWith(val suffix: String) {
    companion object {
        internal object Factory : Constraint.Factory<EndsWith, String?> {
            override fun make(data: EndsWith, type: Type): Constraint<String?> = { value ->
                if (value != null && !value.endsWith(data.suffix)) {
                    throw SerializationException("String must end with '${data.suffix}'")
                }
            }
        }
    }
}