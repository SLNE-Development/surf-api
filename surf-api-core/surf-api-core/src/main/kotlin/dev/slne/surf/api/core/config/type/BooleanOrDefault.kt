package dev.slne.surf.api.core.config.type

import org.apache.commons.lang3.BooleanUtils
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

/**
 * Represents a boolean configuration value that can either be explicitly set to `true` or `false`,
 * or defer to a caller-provided default value.
 *
 * The serialized configuration values are:
 * - `true`
 * - `false`
 * - `default`
 *
 * Usage:
 * ```kotlin
 * @ConfigSerializable
 * data class FeatureConfig(
 *     val enabled: BooleanOrDefault = BooleanOrDefault.USE_DEFAULT
 * )
 *
 * val enabled = config.enabled or globalDefaultEnabled
 * ```
 */
@ConsistentCopyVisibility
data class BooleanOrDefault private constructor(val value: Boolean?) {

    /**
     * Returns the configured boolean value, or [other] if this value is configured as `default`.
     */
    infix fun or(other: Boolean) = value ?: other

    companion object {

        /**
         * Represents the `default` configuration value.
         */
        @JvmField
        val USE_DEFAULT = BooleanOrDefault(null)

        /**
         * Represents an explicitly enabled configuration value.
         */
        @JvmField
        val TRUE = BooleanOrDefault(true)

        /**
         * Represents an explicitly disabled configuration value.
         */
        @JvmField
        val FALSE = BooleanOrDefault(false)
    }

    /**
     * Configurate serializer for [BooleanOrDefault].
     */
    internal object Serializer : ScalarSerializer.Annotated<BooleanOrDefault>(BooleanOrDefault::class.java) {
        private const val DEFAULT_VALUE = "default"

        override fun deserialize(type: AnnotatedType, obj: Any): BooleanOrDefault {
            if (obj is String) {
                if (obj.equals(DEFAULT_VALUE, ignoreCase = true)) {
                    return USE_DEFAULT
                }

                try {
                    return BooleanOrDefault(BooleanUtils.toBoolean(obj.lowercase(), "true", "false"))
                } catch (e: IllegalArgumentException) {
                    throw SerializationException(
                        BooleanOrDefault::class.java,
                        "$obj($type) is not a boolean or '$DEFAULT_VALUE'",
                        e
                    )
                }
            } else if (obj is Boolean) {
                return BooleanOrDefault(obj)
            }

            throw SerializationException(BooleanOrDefault::class.java, "$obj($type) is not a boolean or '$DEFAULT_VALUE'")
        }

        override fun serialize(type: AnnotatedType, item: BooleanOrDefault, typeSupported: Predicate<Class<*>>): Any {
            return item.value ?: DEFAULT_VALUE
        }
    }
}