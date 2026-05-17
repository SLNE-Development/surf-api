package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Ensures that a configuration value represents a valid, existing directory.
 *
 * This annotation validates that the annotated value points to a directory that exists on the filesystem.
 * If the value does not exist, or if it does not represent a directory, validation will fail with a
 * `SerializationException`.
 *
 * This constraint supports different types of input, such as `Path`, `File`, and `String`, converting
 * them to a `Path` internally using the helper method `asPathOrNull`.
 *
 * Constraints:
 * - The value must point to an existing directory.
 * - The value must be convertible to a `Path` object.
 *
 * Intended for use on fields in configuration classes.
 *
 * Validation failure will throw a `SerializationException` with a descriptive message indicating the
 * problem with the directory path.
 *
 * Associated factory implementation provides the actual validation logic.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Directory {
    companion object {
        internal object Factory : Constraint.Factory<Directory, Any?> {
            override fun make(data: Directory, type: Type): Constraint<Any?> = Constraint { value ->
                val path = value.asPathOrNull() ?: return@Constraint
                if (!path.exists() || !path.isDirectory()) {
                    throw SerializationException("Path must point to an existing directory: $path")
                }
            }
        }
    }
}