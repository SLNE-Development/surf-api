package dev.slne.surf.nats

sealed interface Answer<out T> {
    data class Of<T>(val value: T) : Answer<T>
    data object None : Answer<Nothing>
}