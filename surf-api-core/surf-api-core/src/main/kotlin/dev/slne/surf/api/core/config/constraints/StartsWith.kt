package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Validates that a string config value starts with a specified prefix.
 *
 * This annotation ensures that the annotated string begins with the provided prefix.
 * If the validation fails, a `SerializationException` is thrown with a descriptive error message.
 *
 * @property prefix The prefix that the string value must start with.
 */
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
