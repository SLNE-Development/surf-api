package dev.slne.surf.api.core.environment

/**
 * Validates one parsed environment-variable value.
 *
 * Validation is performed immediately for direct reads and during the first access for delegated
 * reads. Use [require] to add rules inside a typed environment-variable declaration.
 */
class EnvironmentVariableValidation<T> internal constructor(
    private val variableName: String,
    private val rawValue: String?,
    private val sensitive: Boolean,
    private val value: T
) {
    /**
     * Requires [predicate] to accept the resolved value.
     */
    fun require(predicate: (T) -> Boolean) {
        require("the value did not satisfy the required condition", predicate)
    }

    /**
     * Requires [predicate] to accept the resolved value and reports [message] if it does not.
     */
    fun require(message: String, predicate: (T) -> Boolean) {
        val accepted = try {
            predicate(value)
        } catch (exception: Exception) {
            throw failure(
                exception.message ?: "the validation rule threw ${exception::class.simpleName}",
                exception
            )
        }

        if (!accepted) {
            throw failure(message)
        }
    }

    internal fun failure(
        message: String,
        cause: Throwable? = null
    ): EnvironmentVariableValidationException {
        return EnvironmentVariableValidationException(
            variableName = variableName,
            reason = message,
            rawValue = rawValue.takeUnless { sensitive },
            sensitive = sensitive,
            cause = cause
        )
    }
}

/**
 * Requires the resolved value to be inside the inclusive [range].
 */
fun <T : Comparable<T>> EnvironmentVariableValidation<T>.requireIn(range: ClosedRange<T>) {
    require("expected a value in the range $range") { it in range }
}
