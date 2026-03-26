package dev.slne.surf.surfapi.core.api.util

import kotlinx.serialization.Serializable

/**
 * Represents an error in a serializable structure, which can encapsulate information about
 * the type, an optional message, and an optional nested cause.
 *
 * This class is primarily used for the structured serialization of error information and offers
 * the ability to generate a throwable representation of the error.
 *
 * @property type The type or category of the error, describing the nature of the issue.
 * @property message An optional detailed message providing more context about the error.
 * @property cause An optional nested instance of `SerializableError` representing the underlying cause of the error.
 */
@Serializable
data class SerializableError(
    val type: String,
    val message: String? = null,
    val cause: SerializableError? = null
) {

    /**
     * Constructs a Throwable representation of the SerializableError instance.
     * The resulting Throwable includes the error type as its primary message, optionally followed
     * by the detailed message if provided. If the error has a nested cause, it recursively builds
     * a Throwable for the cause as well.
     *
     * @return A Throwable instance representing the SerializableError, including its type,
     *         optional message, and optional cause.
     */
    fun buildFakeThrowable(): Throwable {
        val throwableMessage = buildString {
            append(type)
            if (!message.isNullOrBlank()) {
                append(": ")
                append(message)
            }
        }

        return RuntimeException(throwableMessage, cause?.buildFakeThrowable())
    }
}

/**
 * Converts a `Throwable` instance into a `SerializableError` representation.
 * This allows for structured serialization of exception details, including the type,
 * message, and nested causes of the original `Throwable`.
 *
 * @return A `SerializableError` containing the type of the `Throwable`, its message,
 *         and recursively serialized causes (if any).
 */
fun Throwable.toSerializableError(): SerializableError = SerializableError(
    type = this::class.qualifiedName ?: this::class.simpleName ?: "UnknownThrowable",
    message = message,
    cause = cause?.toSerializableError()
)