package dev.slne.surf.api.core.config.constraints

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
                if (number != null && number.toDouble() <= 0) {
                    throw SerializationException("Number is not positive: $number, expected > 0")
                }
            }
        }
    }
}
