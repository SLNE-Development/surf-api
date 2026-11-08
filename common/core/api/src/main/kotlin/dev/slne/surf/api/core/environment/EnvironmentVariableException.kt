package dev.slne.surf.api.core.environment

/**
 * Base class for failures encountered while resolving an environment variable.
 *
 * @property variableName The name of the environment variable that could not be resolved.
 */
sealed class EnvironmentVariableException(
    val variableName: String,
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/**
 * Thrown when a required environment variable is missing.
 */
class MissingEnvironmentVariableException(
    variableName: String
) : EnvironmentVariableException(
    variableName,
    "Required environment variable '$variableName' is missing."
)

/**
 * Thrown when an environment variable cannot be converted to its expected type.
 *
 * For sensitive variables, [rawValue] and [cause] are deliberately omitted because parser
 * exceptions can include their input in the exception message.
 *
 * @property expectedType A human-readable description of the expected target type.
 * @property rawValue The invalid raw value, or `null` when the variable is sensitive.
 */
class InvalidEnvironmentVariableException internal constructor(
    variableName: String,
    val expectedType: String,
    val rawValue: String?,
    sensitive: Boolean,
    cause: Throwable
) : EnvironmentVariableException(
    variableName,
    buildString {
        append("Environment variable '")
        append(variableName)
        append("' has an invalid value for ")
        append(expectedType)
        append(": ")
        if (sensitive) {
            append("the sensitive value was not included.")
        } else {
            append(formatRawValue(rawValue.orEmpty()))
            append('.')
        }
    },
    cause.takeUnless { sensitive }
)

/**
 * Thrown when a parsed environment-variable value fails inline validation.
 *
 * @property reason A description of the failed validation rule.
 * @property rawValue The resolved value, or `null` when the variable is sensitive.
 */
class EnvironmentVariableValidationException internal constructor(
    variableName: String,
    val reason: String,
    val rawValue: String?,
    sensitive: Boolean,
    cause: Throwable? = null
) : EnvironmentVariableException(
    variableName,
    buildString {
        append("Environment variable '")
        append(variableName)
        append("' failed validation: ")
        append(reason)
        if (sensitive) {
            append(" The sensitive value was not included.")
        } else if (rawValue != null) {
            append(" Resolved ")
            append(formatRawValue(rawValue))
            append('.')
        }
    },
    cause.takeUnless { sensitive }
)

private fun formatRawValue(value: String): String {
    val truncated = value.length > MAX_DISPLAYED_VALUE_LENGTH
    val escaped = buildString {
        value.take(MAX_DISPLAYED_VALUE_LENGTH).forEach { character ->
            when (character) {
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }

    return if (truncated) {
        "value '$escaped…'"
    } else {
        "value '$escaped'"
    }
}

private const val MAX_DISPLAYED_VALUE_LENGTH = 128
