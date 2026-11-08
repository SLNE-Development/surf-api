package dev.slne.surf.api.core.config.type.number

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

/**
 * Represents an optional double configuration value.
 *
 * Implementations decide what the empty state means:
 * - [Default] uses `default`
 * - [Disabled] uses `disabled`
 *
 * Usage:
 * ```kotlin
 * @ConfigSerializable
 * data class SpawnConfig(
 *     val multiplier: DoubleOr.Default = DoubleOr.Default.USE_DEFAULT,
 *     val customChance: DoubleOr.Disabled = DoubleOr.Disabled.DISABLED
 * )
 *
 * val multiplier = config.multiplier or 1.0
 *
 * if (config.customChance.test { it > 0.0 }) {
 *     val chance = config.customChance.doubleValue()
 * }
 * ```
 */
interface DoubleOr {

    /**
     * Returns the configured double value, or [fallback] if this value is empty.
     */
    infix fun or(fallback: Double): Double

    /**
     * The configured double value, or `null` if this value is empty.
     */
    val value: Double?

    /**
     * Returns the configured double value.
     *
     * @throws NullPointerException if this value is empty.
     */
    fun doubleValue(): Double = value!!

    /**
     * Double value that can be configured as either a concrete number or `default`.
     *
     * Usage:
     * ```kotlin
     * @ConfigSerializable
     * data class Config(
     *     val multiplier: DoubleOr.Default = DoubleOr.Default.USE_DEFAULT
     * )
     *
     * val multiplier = config.multiplier or 1.0
     * ```
     */
    data class Default(
        override val value: Double?
    ) : DoubleOr {

        /**
         * Returns the configured double value, or [fallback] if this value is `default`.
         */
        override fun or(fallback: Double) = value ?: fallback

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
                val value = parseDouble(type, obj, DEFAULT_VALUE) ?: return USE_DEFAULT

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
     * Double value that can be configured as either a concrete number or `disabled`.
     *
     * Usage:
     * ```kotlin
     * @ConfigSerializable
     * data class Config(
     *     val threshold: DoubleOr.Disabled = DoubleOr.Disabled.DISABLED
     * )
     *
     * if (config.threshold.enabled()) {
     *     val threshold = config.threshold.doubleValue()
     * }
     * ```
     */
    data class Disabled(
        override val value: Double?
    ) : DoubleOr {

        /**
         * Returns the configured double value, or [fallback] if this value is `disabled`.
         */
        override fun or(fallback: Double) = value ?: fallback

        /**
         * Returns `true` if this value contains an explicit double.
         */
        fun enabled(): Boolean = value != null

        /**
         * Returns `true` if this value is enabled and its double value matches [predicate].
         */
        fun test(predicate: (Double) -> Boolean): Boolean {
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
        object Serializer : ScalarSerializer.Annotated<Disabled>(Disabled::class.java) {
            override fun deserialize(type: AnnotatedType, obj: Any): Disabled {
                val value = parseDouble(type, obj, DISABLED_VALUE) ?: return DISABLED

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

private fun parseDouble(type: AnnotatedType, obj: Any, emptyValue: String): Double? {
    return when (obj) {
        is String -> {
            if (obj.equals(emptyValue, ignoreCase = true)) {
                null
            } else {
                obj.toDoubleOrNull()
                    ?: throw SerializationException(
                        Double::class.java,
                        "$obj($type) is not a double or '$emptyValue'"
                    )
            }
        }

        is Number -> obj.toDouble()

        else -> throw SerializationException(
            Double::class.java,
            "$obj($type) is not a double or '$emptyValue'"
        )
    }
}