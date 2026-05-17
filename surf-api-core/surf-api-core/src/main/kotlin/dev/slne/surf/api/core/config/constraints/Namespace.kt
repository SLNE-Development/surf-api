package dev.slne.surf.api.core.config.constraints

import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Ensures that a key value conforms to a specific namespace.
 *
 * This annotation validates that the `namespace` of a key matches the expected namespace
 * defined in the `Namespace` annotation. If the validation fails, a `SerializationException`
 * is thrown with a descriptive error message.
 *
 * @property namespace The required namespace for the key value.
 */
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