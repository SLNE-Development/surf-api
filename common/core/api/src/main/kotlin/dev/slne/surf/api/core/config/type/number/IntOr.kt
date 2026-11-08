package dev.slne.surf.api.core.config.type.number

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

/**
 * Represents an optional integer configuration value.
 *
 * Implementations decide what the empty state means:
 * - [Default] uses `default`
 * - [Disabled] uses `disabled`
 *
 * Usage:
 * ```kotlin
 * @ConfigSerializable
 * data class RetryConfig(
 *     val maxRetries: IntOr.Default = IntOr.Default.USE_DEFAULT,
 *     val queueLimit: IntOr.Disabled = IntOr.Disabled.DISABLED
 * )
 *
 * val retries = config.maxRetries or 3
 *
 * if (config.queueLimit.enabled()) {
 *     val limit = config.queueLimit.intValue()
 * }
 * ```
 */
interface IntOr {

    /**
     * Returns the configured integer value, or [fallback] if this value is empty.
     */
    infix fun or(fallback: Int): Int

    /**
     * The configured integer value, or `null` if this value is empty.
     */
    val value: Int?

    /**
     * Returns `true` if this value contains an explicit integer.
     */
    fun isDefined(): Boolean = value != null

    /**
     * Returns the configured integer value.
     *
     * @throws NullPointerException if this value is empty.
     */
    fun intValue(): Int = value!!

    /**
     * Integer value that can be configured as either a concrete number or `default`.
     *
     * Usage:
     * ```kotlin
     * @ConfigSerializable
     * data class Config(
     *     val amount: IntOr.Default = IntOr.Default.USE_DEFAULT
     * )
     *
     * val amount = config.amount or 10
     * ```
     */
    data class Default(override val value: Int?) : IntOr {

        /**
         * Returns the configured integer value, or [fallback] if this value is `default`.
         */
        override fun or(fallback: Int) = value ?: fallback

        companion object {
            private const val DEFAULT_VALUE = "default"

            /**
             * Represents the `default` configuration value.
             */
            @JvmField
            val USE_DEFAULT = Default(null)
        }

        /**
         * Configurate serializer for [Default].
         */
        internal object Serializer : ScalarSerializer.Annotated<Default>(Default::class.java) {
            override fun deserialize(type: AnnotatedType, obj: Any): Default {
                val value = parseInt(type, obj, DEFAULT_VALUE) ?: return USE_DEFAULT

                if (type.isAnnotationPresent(BelowZeroToEmpty::class.java) && value < 0) {
                    return USE_DEFAULT
                }

                return Default(value)
            }

            override fun serialize(
                type: AnnotatedType,
                item: Default,
                typeSupported: Predicate<Class<*>>
            ): Any {
                return item.value ?: DEFAULT_VALUE
            }
        }
    }

    /**
     * Integer value that can be configured as either a concrete number or `disabled`.
     *
     * Usage:
     * ```kotlin
     * @ConfigSerializable
     * data class Config(
     *     val limit: IntOr.Disabled = IntOr.Disabled.DISABLED
     * )
     *
     * if (config.limit.enabled()) {
     *     val limit = config.limit.intValue()
     * }
     * ```
     */
    data class Disabled(
        override val value: Int?
    ) : IntOr {
        /**
         * Returns the configured integer value, or [fallback] if this value is `disabled`.
         */
        override fun or(fallback: Int) = value ?: fallback

        /**
         * Returns `true` if this value contains an explicit integer.
         */
        fun enabled(): Boolean = value != null

        /**
         * Returns `true` if this value is enabled and its integer value matches [predicate].
         */
        inline fun test(predicate: (Int) -> Boolean): Boolean {
            return value?.let(predicate) ?: false
        }

        companion object {
            private const val DISABLED_VALUE = "disabled"

            /**
             * Represents the `disabled` configuration value.
             */
            @JvmField
            val DISABLED = Disabled(null)
        }

        /**
         * Configurate serializer for [Disabled].
         */
        internal object Serializer : ScalarSerializer.Annotated<Disabled>(Disabled::class.java) {
            override fun deserialize(type: AnnotatedType, obj: Any): Disabled {
                val value = parseInt(type, obj, DISABLED_VALUE) ?: return DISABLED

                if (type.isAnnotationPresent(BelowZeroToEmpty::class.java) && value < 0) {
                    return DISABLED
                }

                return Disabled(value)
            }

            override fun serialize(
                type: AnnotatedType,
                item: Disabled,
                typeSupported: Predicate<Class<*>>
            ): Any {
                return item.value ?: DISABLED_VALUE
            }
        }
    }
}

private fun parseInt(type: AnnotatedType, obj: Any, emptyValue: String): Int? {
    return when (obj) {
        is String -> {
            if (obj.equals(emptyValue, ignoreCase = true)) {
                null
            } else {
                obj.toIntOrNull()
                    ?: throw SerializationException(
                        Int::class.java,
                        "$obj($type) is not an int or '$emptyValue'"
                    )
            }
        }

        is Number -> obj.toInt()

        else -> throw SerializationException(
            Int::class.java,
            "$obj($type) is not an int or '$emptyValue'"
        )
    }
}