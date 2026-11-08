package dev.slne.surf.api.core.environment

import java.util.UUID
import kotlin.time.Duration

/**
 * Resolves, converts, and validates environment variables without a separate configuration schema.
 *
 * Direct functions resolve immediately. Overloads without a variable name return lazy,
 * thread-safe delegates that infer the name from the property. Use [EnvironmentVariableDelegate.named]
 * when a delegated property's environment-variable name differs from its Kotlin property name.
 */
class EnvironmentVariables private constructor(
    private val source: EnvironmentSource
) {
    /**
     * Resolves a required string. Present blank values are returned unchanged.
     */
    fun required(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): String = requiredConverted(name, "String", sensitive, { it }, validation)

    /**
     * Resolves a required value using [converter].
     */
    inline fun <reified T> required(
        name: String,
        noinline converter: (String) -> T,
        sensitive: Boolean = false,
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): T = requiredConverted(name, targetTypeName<T>(), sensitive, converter, validation)

    /**
     * Resolves an optional string, returning `null` only when it is missing.
     */
    fun optional(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): String? = optionalConverted(name, "String", sensitive, { it }, validation)

    /**
     * Resolves an optional value using [converter], returning `null` when it is missing.
     */
    inline fun <reified T> optional(
        name: String,
        noinline converter: (String) -> T,
        sensitive: Boolean = false,
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): T? = optionalConverted(name, targetTypeName<T>(), sensitive, converter, validation)

    /**
     * Resolves a required string and rejects blank values.
     */
    fun requiredNonBlank(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): String = required(name, sensitive) {
        require("expected a non-blank string") { it.isNotBlank() }
        validation()
    }

    /**
     * Resolves a required sensitive string without including its value in failures.
     *
     * Like [required], this allows blank strings. Combine `requiredNonBlank` with
     * `sensitive = true` when both guarantees are needed.
     */
    fun requiredSecret(
        name: String,
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): String = required(name, sensitive = true, validation = validation)

    /**
     * Resolves a string or returns [default] when it is missing.
     */
    fun string(
        name: String,
        default: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): String = defaultedConverted(name, default, "String", sensitive, { it }, validation)

    /** Resolves a required [Int]. */
    fun int(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Int>.() -> Unit = {}
    ): Int = requiredConverted(name, "Int", sensitive, String::toInt, validation)

    /** Resolves an optional [Int]. */
    fun optionalInt(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Int>.() -> Unit = {}
    ): Int? = optionalConverted(name, "Int", sensitive, String::toInt, validation)

    /** Resolves an [Int] or returns [default] when it is missing. */
    fun int(
        name: String,
        default: Int,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Int>.() -> Unit = {}
    ): Int = defaultedConverted(name, default, "Int", sensitive, String::toInt, validation)

    /** Resolves a required [Long]. */
    fun long(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Long>.() -> Unit = {}
    ): Long = requiredConverted(name, "Long", sensitive, String::toLong, validation)

    /** Resolves an optional [Long]. */
    fun optionalLong(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Long>.() -> Unit = {}
    ): Long? = optionalConverted(name, "Long", sensitive, String::toLong, validation)

    /** Resolves a [Long] or returns [default] when it is missing. */
    fun long(
        name: String,
        default: Long,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Long>.() -> Unit = {}
    ): Long = defaultedConverted(name, default, "Long", sensitive, String::toLong, validation)

    /** Resolves a required [Double]. */
    fun double(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Double>.() -> Unit = {}
    ): Double = requiredConverted(name, "Double", sensitive, String::toDouble, validation)

    /** Resolves an optional [Double]. */
    fun optionalDouble(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Double>.() -> Unit = {}
    ): Double? = optionalConverted(name, "Double", sensitive, String::toDouble, validation)

    /** Resolves a [Double] or returns [default] when it is missing. */
    fun double(
        name: String,
        default: Double,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Double>.() -> Unit = {}
    ): Double = defaultedConverted(name, default, "Double", sensitive, String::toDouble, validation)

    /** Resolves a required [Float]. */
    fun float(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Float>.() -> Unit = {}
    ): Float = requiredConverted(name, "Float", sensitive, String::toFloat, validation)

    /** Resolves an optional [Float]. */
    fun optionalFloat(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Float>.() -> Unit = {}
    ): Float? = optionalConverted(name, "Float", sensitive, String::toFloat, validation)

    /** Resolves a [Float] or returns [default] when it is missing. */
    fun float(
        name: String,
        default: Float,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Float>.() -> Unit = {}
    ): Float = defaultedConverted(name, default, "Float", sensitive, String::toFloat, validation)

    /**
     * Resolves a required strict boolean.
     *
     * Only `true` and `false` are accepted, case-insensitively.
     */
    fun boolean(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Boolean>.() -> Unit = {}
    ): Boolean = requiredConverted(name, "Boolean", sensitive, ::parseBoolean, validation)

    /** Resolves an optional strict boolean. */
    fun optionalBoolean(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Boolean>.() -> Unit = {}
    ): Boolean? = optionalConverted(name, "Boolean", sensitive, ::parseBoolean, validation)

    /** Resolves a strict boolean or returns [default] when it is missing. */
    fun boolean(
        name: String,
        default: Boolean,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Boolean>.() -> Unit = {}
    ): Boolean = defaultedConverted(name, default, "Boolean", sensitive, ::parseBoolean, validation)

    /** Resolves a required [UUID]. */
    fun uuid(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<UUID>.() -> Unit = {}
    ): UUID = requiredConverted(name, "UUID", sensitive, UUID::fromString, validation)

    /** Resolves an optional [UUID]. */
    fun optionalUuid(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<UUID>.() -> Unit = {}
    ): UUID? = optionalConverted(name, "UUID", sensitive, UUID::fromString, validation)

    /** Resolves a [UUID] or returns [default] when it is missing. */
    fun uuid(
        name: String,
        default: UUID,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<UUID>.() -> Unit = {}
    ): UUID = defaultedConverted(name, default, "UUID", sensitive, UUID::fromString, validation)

    /** Resolves a required Kotlin [Duration]. */
    fun duration(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Duration>.() -> Unit = {}
    ): Duration = requiredConverted(name, "Duration", sensitive, Duration::parse, validation)

    /** Resolves an optional Kotlin [Duration]. */
    fun optionalDuration(
        name: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Duration>.() -> Unit = {}
    ): Duration? = optionalConverted(name, "Duration", sensitive, Duration::parse, validation)

    /** Resolves a Kotlin [Duration] or returns [default] when it is missing. */
    fun duration(
        name: String,
        default: Duration,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<Duration>.() -> Unit = {}
    ): Duration = defaultedConverted(name, default, "Duration", sensitive, Duration::parse, validation)

    /** Resolves a required enum constant by its exact constant name. */
    inline fun <reified T : Enum<T>> enum(
        name: String,
        sensitive: Boolean = false,
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): T = required(name, { enumValueOf<T>(it) }, sensitive, validation)

    /** Resolves an optional enum constant by its exact constant name. */
    inline fun <reified T : Enum<T>> optionalEnum(
        name: String,
        sensitive: Boolean = false,
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): T? = optional(name, { enumValueOf<T>(it) }, sensitive, validation)

    /** Resolves an enum constant or returns [default] when it is missing. */
    inline fun <reified T : Enum<T>> enum(
        name: String,
        default: T,
        sensitive: Boolean = false,
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): T = defaultedConverted(
        name,
        default,
        targetTypeName<T>(),
        sensitive,
        { enumValueOf<T>(it) },
        validation
    )

    /**
     * Creates a required string delegate using the property name as the variable name.
     */
    fun required(
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): EnvironmentVariableDelegate<String> = delegate { name ->
        required(name, sensitive, validation)
    }

    /**
     * Creates a required converted-value delegate using the property name as the variable name.
     */
    inline fun <reified T> required(
        noinline converter: (String) -> T,
        sensitive: Boolean = false,
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): EnvironmentVariableDelegate<T> = delegate { name ->
        required(name, converter, sensitive, validation)
    }

    /**
     * Creates an optional string delegate using the property name as the variable name.
     */
    fun optional(
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): EnvironmentVariableDelegate<String?> = delegate { name ->
        optional(name, sensitive, validation)
    }

    /**
     * Creates an optional converted-value delegate using the property name as the variable name.
     */
    inline fun <reified T> optional(
        noinline converter: (String) -> T,
        sensitive: Boolean = false,
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): EnvironmentVariableDelegate<T?> = delegate { name ->
        optional(name, converter, sensitive, validation)
    }

    /** Creates a required non-blank string delegate. */
    fun requiredNonBlank(
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): EnvironmentVariableDelegate<String> = delegate { name ->
        requiredNonBlank(name, sensitive, validation)
    }

    /** Creates a required sensitive string delegate. */
    fun requiredSecret(
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): EnvironmentVariableDelegate<String> = required(sensitive = true, validation)

    /** Creates a defaulted string delegate. */
    fun string(
        default: String,
        sensitive: Boolean = false,
        validation: EnvironmentVariableValidation<String>.() -> Unit = {}
    ): EnvironmentVariableDelegate<String> = delegate { name ->
        string(name, default, sensitive, validation)
    }

    /** Creates a required [Int] delegate. */
    fun int(
        validation: EnvironmentVariableValidation<Int>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Int> = delegate { name -> int(name, validation = validation) }

    /** Creates an optional [Int] delegate. */
    fun optionalInt(
        validation: EnvironmentVariableValidation<Int>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Int?> = delegate { name ->
        optionalInt(name, validation = validation)
    }

    /** Creates a defaulted [Int] delegate. */
    fun int(
        default: Int,
        validation: EnvironmentVariableValidation<Int>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Int> = delegate { name -> int(name, default, validation = validation) }

    /** Creates a required [Long] delegate. */
    fun long(
        validation: EnvironmentVariableValidation<Long>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Long> = delegate { name -> long(name, validation = validation) }

    /** Creates an optional [Long] delegate. */
    fun optionalLong(
        validation: EnvironmentVariableValidation<Long>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Long?> = delegate { name ->
        optionalLong(name, validation = validation)
    }

    /** Creates a defaulted [Long] delegate. */
    fun long(
        default: Long,
        validation: EnvironmentVariableValidation<Long>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Long> = delegate { name ->
        long(name, default, validation = validation)
    }

    /** Creates a required [Double] delegate. */
    fun double(
        validation: EnvironmentVariableValidation<Double>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Double> = delegate { name -> double(name, validation = validation) }

    /** Creates an optional [Double] delegate. */
    fun optionalDouble(
        validation: EnvironmentVariableValidation<Double>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Double?> = delegate { name ->
        optionalDouble(name, validation = validation)
    }

    /** Creates a defaulted [Double] delegate. */
    fun double(
        default: Double,
        validation: EnvironmentVariableValidation<Double>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Double> = delegate { name ->
        double(name, default, validation = validation)
    }

    /** Creates a required [Float] delegate. */
    fun float(
        validation: EnvironmentVariableValidation<Float>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Float> = delegate { name -> float(name, validation = validation) }

    /** Creates an optional [Float] delegate. */
    fun optionalFloat(
        validation: EnvironmentVariableValidation<Float>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Float?> = delegate { name ->
        optionalFloat(name, validation = validation)
    }

    /** Creates a defaulted [Float] delegate. */
    fun float(
        default: Float,
        validation: EnvironmentVariableValidation<Float>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Float> = delegate { name ->
        float(name, default, validation = validation)
    }

    /** Creates a required strict-boolean delegate. */
    fun boolean(
        validation: EnvironmentVariableValidation<Boolean>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Boolean> = delegate { name ->
        boolean(name, validation = validation)
    }

    /** Creates an optional strict-boolean delegate. */
    fun optionalBoolean(
        validation: EnvironmentVariableValidation<Boolean>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Boolean?> = delegate { name ->
        optionalBoolean(name, validation = validation)
    }

    /** Creates a defaulted strict-boolean delegate. */
    fun boolean(
        default: Boolean,
        validation: EnvironmentVariableValidation<Boolean>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Boolean> = delegate { name ->
        boolean(name, default, validation = validation)
    }

    /** Creates a required [UUID] delegate. */
    fun uuid(
        validation: EnvironmentVariableValidation<UUID>.() -> Unit = {}
    ): EnvironmentVariableDelegate<UUID> = delegate { name -> uuid(name, validation = validation) }

    /** Creates an optional [UUID] delegate. */
    fun optionalUuid(
        validation: EnvironmentVariableValidation<UUID>.() -> Unit = {}
    ): EnvironmentVariableDelegate<UUID?> = delegate { name ->
        optionalUuid(name, validation = validation)
    }

    /** Creates a defaulted [UUID] delegate. */
    fun uuid(
        default: UUID,
        validation: EnvironmentVariableValidation<UUID>.() -> Unit = {}
    ): EnvironmentVariableDelegate<UUID> = delegate { name ->
        uuid(name, default, validation = validation)
    }

    /** Creates a required Kotlin [Duration] delegate. */
    fun duration(
        validation: EnvironmentVariableValidation<Duration>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Duration> = delegate { name ->
        duration(name, validation = validation)
    }

    /** Creates an optional Kotlin [Duration] delegate. */
    fun optionalDuration(
        validation: EnvironmentVariableValidation<Duration>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Duration?> = delegate { name ->
        optionalDuration(name, validation = validation)
    }

    /** Creates a defaulted Kotlin [Duration] delegate. */
    fun duration(
        default: Duration,
        validation: EnvironmentVariableValidation<Duration>.() -> Unit = {}
    ): EnvironmentVariableDelegate<Duration> = delegate { name ->
        duration(name, default, validation = validation)
    }

    /** Creates a required exact-name enum delegate. */
    inline fun <reified T : Enum<T>> enum(
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): EnvironmentVariableDelegate<T> = delegate { name -> enum<T>(name, validation = validation) }

    /** Creates an optional exact-name enum delegate. */
    inline fun <reified T : Enum<T>> optionalEnum(
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): EnvironmentVariableDelegate<T?> = delegate { name ->
        optionalEnum<T>(name, validation = validation)
    }

    /** Creates a defaulted exact-name enum delegate. */
    inline fun <reified T : Enum<T>> enum(
        default: T,
        noinline validation: EnvironmentVariableValidation<T>.() -> Unit = {}
    ): EnvironmentVariableDelegate<T> = delegate { name ->
        enum(name, default, validation = validation)
    }

    @PublishedApi
    @JvmSynthetic
    internal fun <T> requiredConverted(
        name: String,
        expectedType: String,
        sensitive: Boolean,
        converter: (String) -> T,
        validation: EnvironmentVariableValidation<T>.() -> Unit
    ): T {
        validateName(name)
        val rawValue = source[name] ?: throw MissingEnvironmentVariableException(name)
        val value = convert(name, rawValue, expectedType, sensitive, converter)
        validate(name, rawValue, sensitive, value, validation)
        return value
    }

    @PublishedApi
    @JvmSynthetic
    internal fun <T> optionalConverted(
        name: String,
        expectedType: String,
        sensitive: Boolean,
        converter: (String) -> T,
        validation: EnvironmentVariableValidation<T>.() -> Unit
    ): T? {
        validateName(name)
        val rawValue = source[name] ?: return null
        val value = convert(name, rawValue, expectedType, sensitive, converter)
        validate(name, rawValue, sensitive, value, validation)
        return value
    }

    @PublishedApi
    @JvmSynthetic
    internal fun <T> defaultedConverted(
        name: String,
        default: T,
        expectedType: String,
        sensitive: Boolean,
        converter: (String) -> T,
        validation: EnvironmentVariableValidation<T>.() -> Unit
    ): T {
        validateName(name)
        val rawValue = source[name]
        val value = if (rawValue == null) {
            default
        } else {
            convert(name, rawValue, expectedType, sensitive, converter)
        }

        validate(name, rawValue ?: default.toString(), sensitive, value, validation)
        return value
    }

    @PublishedApi
    @JvmSynthetic
    internal fun <T> delegate(
        explicitName: String? = null,
        resolver: (String) -> T
    ): EnvironmentVariableDelegate<T> = EnvironmentVariableDelegate(explicitName, resolver)

    private fun <T> convert(
        name: String,
        rawValue: String,
        expectedType: String,
        sensitive: Boolean,
        converter: (String) -> T
    ): T {
        return try {
            converter(rawValue)
        } catch (exception: EnvironmentVariableException) {
            throw exception
        } catch (exception: Exception) {
            throw InvalidEnvironmentVariableException(
                variableName = name,
                expectedType = expectedType,
                rawValue = rawValue.takeUnless { sensitive },
                sensitive = sensitive,
                cause = exception
            )
        }
    }

    private fun <T> validate(
        name: String,
        rawValue: String,
        sensitive: Boolean,
        value: T,
        validation: EnvironmentVariableValidation<T>.() -> Unit
    ) {
        val scope = EnvironmentVariableValidation(name, rawValue, sensitive, value)
        try {
            scope.validation()
        } catch (exception: EnvironmentVariableException) {
            throw exception
        } catch (exception: Exception) {
            throw scope.failure(
                exception.message ?: "validation threw ${exception::class.simpleName}",
                exception
            )
        }
    }

    private fun validateName(name: String) {
        require(name.isNotEmpty()) { "Environment-variable names must not be empty." }
        require('\u0000' !in name && '=' !in name) {
            "Environment-variable name '$name' contains an invalid character."
        }
    }

    /**
     * Factory functions and the process-wide default environment.
     */
    companion object {
        /**
         * Environment variables supplied by the current process.
         */
        @JvmField
        val system = EnvironmentVariables(EnvironmentSource(System::getenv))

        /**
         * Creates an environment backed by [source].
         */
        @JvmStatic
        fun from(source: EnvironmentSource): EnvironmentVariables = EnvironmentVariables(source)

        /**
         * Creates an environment backed by a stable copy of [values].
         */
        @JvmStatic
        fun from(values: Map<String, String>): EnvironmentVariables {
            val snapshot = values.toMap()
            return EnvironmentVariables(EnvironmentSource(snapshot::get))
        }
    }
}

/**
 * Convenient process-wide environment-variable accessor.
 */
@JvmField
val env: EnvironmentVariables = EnvironmentVariables.system

@PublishedApi
internal inline fun <reified T> targetTypeName(): String {
    return T::class.simpleName ?: "value"
}

private fun parseBoolean(value: String): Boolean {
    return when {
        value.equals("true", ignoreCase = true) -> true
        value.equals("false", ignoreCase = true) -> false
        else -> throw IllegalArgumentException("Expected either 'true' or 'false'")
    }
}
