package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import kotlin.io.path.exists
import kotlin.io.path.isWritable

/**
 * Validates that a configuration value represents a writable path when the path exists.
 *
 * This annotation ensures that the annotated value refers to a file system path
 * that is writable if it already exists. If the validation fails for an existing path,
 * a `SerializationException` will be thrown with a descriptive error message.
 *
 * Supported value types include:
 * - `Path`: Directly represents a file system path.
 * - `File`: Converted to a `Path` for validation.
 * - `String`: Parsed as a file system path.
 *
 * Validation checks:
 * - If the path exists, it must be writable.
 * - If the value cannot be converted to a valid path, it is ignored during validation.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class WritablePath {
    companion object {
        internal object Factory : Constraint.Factory<WritablePath, Any?> {
            override fun make(data: WritablePath, type: Type): Constraint<Any?> = Constraint { value ->
                val path = value.asPathOrNull() ?: return@Constraint
                if (path.exists() && !path.isWritable()) {
                    throw SerializationException("Path must be writable: $path")
                }
            }
        }
    }
}