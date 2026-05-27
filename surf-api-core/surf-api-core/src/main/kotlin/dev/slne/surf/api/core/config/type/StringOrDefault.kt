package dev.slne.surf.api.core.config.type

import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

/**
 * Represents a configuration value that can either hold a string or use a default fallback value.
 *
 * The class provides utility to handle values that might explicitly contain a string or defer
 * to a caller-provided default value if none is specified.
 *
 * Features:
 * - Allows explicit value encapsulation via the `of` method.
 * - Supports the `USE_DEFAULT` singleton to represent fallback default behavior.
 * - Provides an `or` infix function to resolve the value with a provided default.
 *
 * Serialization:
 * - The `Serializer` handles the string representation of the value for use in configurations.
 * - Serialized values include explicit strings or an internal `__default__` marker for default representation.
 *
 * Companion Object:
 * - Contains helper methods and a constant (`USE_DEFAULT`) for working with default behavior.
 */
@ConsistentCopyVisibility
data class StringOrDefault private constructor(val value: String?) {

    infix fun or(default: String): String = value ?: default

    companion object {
        private const val DEFAULT_MARKER = "__default__"

        val USE_DEFAULT = StringOrDefault(null)

        fun of(value: String) = StringOrDefault(value)
    }

    internal object Serializer : ScalarSerializer.Annotated<StringOrDefault>(StringOrDefault::class.java) {
        override fun deserialize(
            type: AnnotatedType?,
            obj: Any?
        ): StringOrDefault? {
            val value = obj?.toString()

            return if (value == null || value == DEFAULT_MARKER) {
                USE_DEFAULT
            } else {
                StringOrDefault(value)
            }
        }

        override fun serialize(
            type: AnnotatedType?,
            item: StringOrDefault?,
            typeSupported: Predicate<Class<*>?>?
        ): Any {
            return item?.value ?: DEFAULT_MARKER
        }
    }
}