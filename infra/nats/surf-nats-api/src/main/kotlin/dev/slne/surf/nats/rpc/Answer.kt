package dev.slne.surf.nats.rpc

public sealed interface Answer<out T> {
    public data class Of<T>(val value: T) : Answer<T>
    public data object None : Answer<Nothing>
}

public fun <T : Any> Answer<T>.getOrNull(): T? = (this as? Answer.Of)?.value

public inline fun <T> Answer<T>.getOrElse(default: () -> T): T = when (this) {
    is Answer.Of -> value
    is Answer.None -> default()
}

public fun <T : Any> T?.toAnswer(): Answer<T> {
    return if (this == null) Answer.None else Answer.Of(this)
}