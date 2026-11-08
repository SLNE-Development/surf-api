package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.number.DoubleOr
import dev.slne.surf.api.core.config.type.number.IntOr
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Requires a numeric config value to be inside an inclusive range.
 *
 * Usage:
 * ```kotlin
 * @field:Range(min = 0.0, max = 1.0)
 * val chance: Double = 0.5
 * ```
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Range(val min: Double, val max: Double) {
    companion object {
        internal object Factory : Constraint.Factory<Range, Number?> {
            override fun make(data: Range, type: Type): Constraint<Number?> = { value ->
                if (value != null) {
                    val double = value.toDouble()
                    if (double < data.min || double > data.max) {
                        throw SerializationException("Number is out of range: $value, expected ${data.min}..${data.max}")
                    }
                }
            }
        }

        internal object FactoryIntOr : Constraint.Factory<Range, IntOr?> {
            override fun make(data: Range, type: Type): Constraint<IntOr?> = { intOr ->
                val value = intOr?.value

                if (value != null) {
                    val double = value.toDouble()
                    if (double < data.min || double > data.max) {
                        throw SerializationException("Number is out of range: $value, expected ${data.min}..${data.max}")
                    }
                }
            }
        }

        internal object FactoryDoubleOr : Constraint.Factory<Range, DoubleOr?> {
            override fun make(data: Range, type: Type): Constraint<DoubleOr?> = { doubleOr ->
                val value = doubleOr?.value

                if (value != null) {
                    if (value < data.min || value > data.max) {
                        throw SerializationException("Number is out of range: $value, expected ${data.min}..${data.max}")
                    }
                }
            }
        }
    }
}