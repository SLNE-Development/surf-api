package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.StringOrDefault
import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

/**
 * Requires that the annotated field refers to an existing file.
 *
 * This annotation ensures that the value of the annotated field corresponds to
 * a valid file path that exists and is a regular file. If the validation fails,
 * a `SerializationException` is thrown with a descriptive error message.
 *
 * The value can be:
 * - A `Path` object.
 * - A `File` object.
 * - A `String` representing the file path.
 *
 * If the value is not convertible to a file path or the file does not exist
 * or is not a regular file, the validation fails.
 *
 * This annotation is typically used for configuration values to ensure
 * that specified file paths are valid at runtime.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExistingFile {
    companion object {
        internal object Factory : Constraint.Factory<ExistingFile, Any?> {
            override fun make(data: ExistingFile, type: Type): Constraint<Any?> = Constraint { value ->
                val path = value.asPathOrNull() ?: return@Constraint
                if (!path.exists() || !path.isRegularFile()) {
                    throw SerializationException("Path must point to an existing file: $path")
                }
            }
        }
    }
}

internal tailrec fun Any?.asPathOrNull(): Path? {
    return when (this) {
        null -> null
        is Path -> this
        is File -> toPath()
        is String -> runCatching { Path.of(this) }.getOrNull()
        is StringOrDefault -> value?.asPathOrNull()
        else -> null
    }
}