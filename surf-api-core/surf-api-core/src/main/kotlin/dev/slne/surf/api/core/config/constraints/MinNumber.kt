package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import java.lang.reflect.Type

/**
 * Validates that a numeric configuration value is greater than or equal to [min].
 *
 * `null` values are ignored, so this can also be used with nullable numeric fields.
 *
 * Usage:
 * ```kotlin
 * @ConfigSerializable
 * data class EconomyConfig(
 *     @field:MinNumber(0.0)
 *     val startingBalance: Double = 100.0
 * )
 * ```
 *
 * @property min The inclusive minimum value.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinNumber(val min: Double) {
    companion object {

        /**
         * Configurate constraint factory for [MinNumber].
         */
        internal object Factory : Constraint.Factory<MinNumber, Number?> {
            override fun make(data: MinNumber, type: Type): Constraint<Number?> = { number ->
                if (number != null && number.toDouble() < data.min) {
                    throw IllegalArgumentException("Number is too small: $number, expected >= ${data.min}")
                }
            }
        }
    }
}

