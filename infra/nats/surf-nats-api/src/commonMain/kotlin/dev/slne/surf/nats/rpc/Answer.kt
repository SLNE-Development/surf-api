package dev.slne.surf.nats.rpc

/**
 * Represents the result of a service method for which "no instance knows" is a valid outcome.
 *
 * Every [Recipients.ALL_INSTANCES] method with a response message returns [Answer] of that message. Each handler
 * returns [Known] when it has the answer and [Unknown] otherwise. The caller receives the first [Known] of any
 * instance, or [Unknown] when none arrives in time, as described in [Recipients].
 *
 * ```kotlin
 * override suspend fun find(request: FindRequest): Answer<FindReply> =
 *     values[request.key]?.let { FindReply(it) }.toAnswer()
 *
 * val value = finder.find(FindRequest("spawn")).getOrNull()?.value
 * ```
 */
public sealed interface Answer<out T> {
    /**
     * Contains a known answer.
     *
     * @property value the known value, which may be `null` when [T] is nullable.
     */
    public data class Known<T>(val value: T) : Answer<T>

    /**
     * Indicates that no answer is known.
     */
    public data object Unknown : Answer<Nothing>
}

/**
 * Returns the known value, or `null` for [Answer.Unknown].
 */
public fun <T : Any> Answer<T>.getOrNull(): T? = (this as? Answer.Known)?.value

/**
 * Returns the known value, which may be `null` when [T] is nullable, or the result of [default] for [Answer.Unknown].
 */
public inline fun <T> Answer<T>.getOrElse(default: () -> T): T = when (this) {
    is Answer.Known -> value
    is Answer.Unknown -> default()
}

/**
 * Returns this value wrapped in [Answer.Known], or [Answer.Unknown] when it is `null`.
 */
public fun <T : Any> T?.toAnswer(): Answer<T> {
    return if (this == null) Answer.Unknown else Answer.Known(this)
}
