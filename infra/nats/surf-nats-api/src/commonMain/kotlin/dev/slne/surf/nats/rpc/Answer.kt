package dev.slne.surf.nats.rpc

/**
 * Represents either a known value or a valid indication that no answer is known.
 *
 * Primarily used by [Recipients.ALL_INSTANCES] service methods.
 */
public sealed interface Answer<out T> {
    /**
     * Contains a known answer, including null when [T] is nullable.
     *
     * @property value the known value.
     */
    public data class Known<T>(val value: T) : Answer<T>

    /**
     * Indicates that no answer is known.
     */
    public data object Unknown : Answer<Nothing>
}

/**
 * Returns the known value or null for [Answer.Unknown].
 */
public fun <T : Any> Answer<T>.getOrNull(): T? = (this as? Answer.Known)?.value

/**
 * Returns the known value, including null, or evaluates [default] for [Answer.Unknown].
 */
public inline fun <T> Answer<T>.getOrElse(default: () -> T): T = when (this) {
    is Answer.Known -> value
    is Answer.Unknown -> default()
}

/**
 * Wraps a non-null value in [Answer.Known], or returns [Answer.Unknown] for null.
 */
public fun <T : Any> T?.toAnswer(): Answer<T> {
    return if (this == null) Answer.Unknown else Answer.Known(this)
}
