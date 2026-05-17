package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

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

internal fun Any?.asPathOrNull(): Path? {
    return when (this) {
        null -> null
        is Path -> this
        is File -> toPath()
        is String -> Path.of(this)
        else -> null
    }
}