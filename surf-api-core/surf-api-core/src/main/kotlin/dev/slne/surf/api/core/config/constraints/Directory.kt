package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

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