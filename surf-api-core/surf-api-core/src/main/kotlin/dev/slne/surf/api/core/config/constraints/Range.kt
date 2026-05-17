package dev.slne.surf.api.core.config.constraints

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
    }
}