package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Requires a string config value to contain a specific substring.
 *
 * This annotation ensures that the annotated string contains the specified substring.
 * If the validation fails, a `SerializationException` is thrown with a descriptive error message.
 *
 * @property value The substring that must be present in the string value.
 */
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