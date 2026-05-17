package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoDuplicates {
    companion object {
        internal object Factory : Constraint.Factory<NoDuplicates, Any?> {
            override fun make(data: NoDuplicates, type: Type): Constraint<Any?> = Constraint { value ->
                val elements = when (value) {
                    null -> return@Constraint
                    is Iterable<*> -> value.toList()
                    is Array<*> -> value.toList()
                    else -> return@Constraint
                }

                if (elements.size != elements.toSet().size) {
                    throw SerializationException("Collection must not contain duplicate values")
                }
            }
        }
    }
}