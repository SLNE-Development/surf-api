package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.number.DoubleOr
import dev.slne.surf.api.core.config.type.number.IntOr
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Validates that a numeric configuration value is strictly positive.
 *
 * `null` values are ignored, so this can also be used with nullable numeric fields.
 *
 * Usage:
 * ```kotlin
 * @ConfigSerializable
 * data class ServerConfig(
 *     @field:PositiveNumber
 *     val maxPlayers: Int = 100
 * )
 * ```
 *
 * Values less than or equal to zero are rejected.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class PositiveNumber {
    companion object {
        /**
         * Configurate constraint factory for [PositiveNumber].
         */
        internal object Factory : Constraint.Factory<PositiveNumber, Number?> {
            override fun make(data: PositiveNumber, type: Type): Constraint<Number?> = { number ->
                if (number != null && number.toDouble() <= 0.0) {
                    throw SerializationException("Number is not positive: $number, expected > 0")
                }
            }
        }

        internal object FactoryIntOr : Constraint.Factory<PositiveNumber, IntOr?> {
            override fun make(data: PositiveNumber, type: Type): Constraint<IntOr?> = { intOr ->
                val number = intOr?.value

                if (number != null && number <= 0) {
                    throw SerializationException("Number is not positive: $number, expected > 0")
                }
            }
        }

        internal object FactoryDoubleOr : Constraint.Factory<PositiveNumber, DoubleOr?> {
            override fun make(data: PositiveNumber, type: Type): Constraint<DoubleOr?> = { doubleOr ->
                val number = doubleOr?.value

                if (number != null && number <= 0.0) {
                    throw SerializationException("Number is not positive: $number, expected > 0")
                }
            }
        }
    }
}
