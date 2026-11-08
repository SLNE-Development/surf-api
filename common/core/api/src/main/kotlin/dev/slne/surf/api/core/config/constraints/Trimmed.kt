package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Requires a string config value to have no leading or trailing whitespace.
 *
 * This validates only; it does not mutate the loaded value.
 *
 * Usage:
 * ```kotlin
 * @field:Trimmed
 * val id: String = "example"
 * ```
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Trimmed {
    companion object {
        internal object Factory : Constraint.Factory<Trimmed, String?> {
            override fun make(data: Trimmed, type: Type): Constraint<String?> = { value ->
                if (value != null && value != value.trim()) {
                    throw SerializationException("String must not have leading or trailing whitespace")
                }
            }
        }
    }
}
