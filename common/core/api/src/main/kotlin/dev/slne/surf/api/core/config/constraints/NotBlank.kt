package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Requires a string config value to contain at least one non-whitespace character.
 *
 * Usage:
 * ```kotlin
 * @field:NotBlank
 * val name: String = "default"
 * ```
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotBlank {
    companion object {
        internal object Factory : Constraint.Factory<NotBlank, String?> {
            override fun make(data: NotBlank, type: Type): Constraint<String?> = { value ->
                if (value != null && value.isBlank()) {
                    throw SerializationException("String must not be blank")
                }
            }
        }
    }
}
