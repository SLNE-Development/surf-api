package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Ensures that a numeric config value is strictly negative.
 *
 * This annotation validates that the annotated numeric value is less than zero. If the
 * validation fails, a `SerializationException` is thrown with a descriptive error message.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NegativeNumber {
    companion object {
        internal object Factory : Constraint.Factory<NegativeNumber, Number?> {
            override fun make(data: NegativeNumber, type: Type): Constraint<Number?> = { value ->
                if (value != null && value.toDouble() >= 0.0) {
                    throw SerializationException("Number must be negative: $value, expected < 0")
                }
            }
        }
    }
}