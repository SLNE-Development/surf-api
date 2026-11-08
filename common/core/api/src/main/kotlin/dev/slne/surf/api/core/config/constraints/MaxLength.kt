package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.StringOrDefault
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Validates that a string config value does not exceed a specified maximum length.
 *
 * This annotation is used to ensure that the length of the annotated string
 * is less than or equal to the specified maximum value. If the validation fails,
 * a `SerializationException` is thrown with a descriptive error message.
 *
 * @property max The maximum allowed length for the string value.
 */
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

        internal object FactoryStringOrDefault : Constraint.Factory<MaxLength, StringOrDefault?> {
            override fun make(data: MaxLength, type: Type): Constraint<StringOrDefault?> = { stringOrDefault ->
                val value = stringOrDefault?.value
                if (value != null && value.length > data.max) {
                    throw SerializationException("String is too long: ${value.length}, expected <= ${data.max}")
                }
            }
        }
    }
}