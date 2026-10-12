package dev.slne.surf.nats.rpc

public sealed interface Answer<out T> {
    public data class Known<T>(val value: T) : Answer<T>
    public data object Unknown : Answer<Nothing>
}

public fun <T : Any> Answer<T>.getOrNull(): T? = (this as? Answer.Known)?.value

public inline fun <T> Answer<T>.getOrElse(default: () -> T): T = when (this) {
    is Answer.Known -> value
    is Answer.Unknown -> default()
}

public fun <T : Any> T?.toAnswer(): Answer<T> {
    return if (this == null) Answer.Unknown else Answer.Known(this)
}
