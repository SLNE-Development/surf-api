package inlinecheck.fixture

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.shared.api.util.PublishedSurfApi

@InternalSurfApi
fun internalHelper() = 1

@PublishedSurfApi
fun publishedHelper() = 2

@InternalSurfApi
object InternalHolder {
    fun value() = 3
}

class Holder {
    companion object {
        @InternalSurfApi
        val INSTANCE: Int get() = 4
    }
}

inline fun usesInternal() = internalHelper()

inline fun usesPublished() = publishedHelper()

inline fun usesInternalClass() = InternalHolder.value()

inline fun usesInternalProperty() = Holder.INSTANCE

inline fun withDefault(value: Int = internalHelper()) = value

inline fun crossinlineUse(crossinline block: () -> Unit): Runnable = Runnable {
    block()
    internalHelper()
}

@InternalSurfApi
inline fun internalInline() = internalHelper()

@PublishedApi
internal inline fun internalVisibility() = internalHelper()

fun notInline() = internalHelper()
