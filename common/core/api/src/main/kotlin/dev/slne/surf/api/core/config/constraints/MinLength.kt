package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.StringOrDefault
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Ensures that a string configuration value meets a minimum length requirement.
 *
 * The string is considered valid if its length is greater than or equal to the specified [min] value.
 * Null values are ignored, making this annotation compatible with nullable string fields.
 *
 * @property min The inclusive minimum number of characters required for the string.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinLength(val min: Int) {
    companion object {
        internal object Factory : Constraint.Factory<MinLength, String?> {
            override fun make(data: MinLength, type: Type): Constraint<String?> = { value ->
                if (value != null && value.length < data.min) {
                    throw SerializationException("String is too short: ${value.length}, expected >= ${data.min}")
                }
            }
        }

        internal object FactoryStringOrDefault : Constraint.Factory<MinLength, StringOrDefault?> {
            override fun make(data: MinLength, type: Type): Constraint<StringOrDefault?> = { stringOrDefault ->
                val value = stringOrDefault?.value

                if (value != null && value.length < data.min) {
                    throw SerializationException("String is too short: ${value.length}, expected >= ${data.min}")
                }
            }
        }
    }
}
