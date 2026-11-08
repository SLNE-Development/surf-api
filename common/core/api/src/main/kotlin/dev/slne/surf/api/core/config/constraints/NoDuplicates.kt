package dev.slne.surf.api.core.config.constraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

/**
 * Ensures that a collection or array does not contain duplicate elements.
 *
 * This annotation validates that the annotated value, if it is a collection or array,
 * contains only unique elements. If duplicates are found, a `SerializationException`
 * is thrown with a descriptive error message.
 *
 * Supported types:
 * - `Iterable<?>`: Validates all elements in the iterable.
 * - `Array<?>`: Validates all elements in the array.
 *
 * Values that are `null` or of unsupported types are ignored during validation.
 *
 * Exceptions:
 * - `SerializationException`: Thrown if the collection or array contains duplicate elements.
 */
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