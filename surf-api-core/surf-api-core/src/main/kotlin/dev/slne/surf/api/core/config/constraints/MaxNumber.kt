package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Validates that a numeric configuration value is less than or equal to [max].
 *
 * `null` values are ignored, so this can also be used with nullable numeric fields.
 *
 * Usage:
 * ```kotlin
 * @ConfigSerializable
 * data class RateLimitConfig(
 *     @field:MaxNumber(100.0)
 *     val percentage: Double = 50.0
 * )
 * ```
 *
 * @property max The inclusive maximum value.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MaxNumber(val max: Double) {
    companion object {

        /**
         * Configurate constraint factory for [MaxNumber].
         */
        internal object Factory : Constraint.Factory<MaxNumber, Number?> {
            override fun make(data: MaxNumber, type: Type): Constraint<Number?> = { number ->
                if (number != null && number.toDouble() > data.max) {
                    throw SerializationException(type, "Number is too big: $number, expected <= ${data.max}")
                }
            }
        }
    }
}
