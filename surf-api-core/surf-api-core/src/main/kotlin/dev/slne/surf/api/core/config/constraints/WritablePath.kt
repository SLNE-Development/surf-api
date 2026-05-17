package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import kotlin.io.path.exists
import kotlin.io.path.isWritable

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