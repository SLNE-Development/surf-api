package dev.slne.surf.api.core.config.constraints

import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Namespace(val namespace: String) {
    companion object {
        internal object Factory : Constraint.Factory<Namespace, Key?> {
            override fun make(data: Namespace, type: Type): Constraint<Key?> = { value ->
                if (value != null && value.namespace() != data.namespace) {
                    throw SerializationException("Key must use namespace '${data.namespace}', got '${value.namespace()}'")
                }
            }
        }
    }
}