package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.StringOrDefault
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Validates that a string config value ends with a specified suffix.
 *
 * This annotation ensures that the annotated string ends with the provided suffix.
 * If the validation fails, a `SerializationException` is thrown with a descriptive error message.
 *
 * @property suffix The suffix that the string value must end with.
 */
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

        internal object FactoryStringOrDefault : Constraint.Factory<EndsWith, StringOrDefault?> {
            override fun make(data: EndsWith, type: Type): Constraint<StringOrDefault?> = { stringOrDefault ->
                val value = stringOrDefault?.value
                if (value != null && !value.endsWith(data.suffix)) {
                    throw SerializationException("String must end with '${data.suffix}'")
                }
            }
        }
    }
}