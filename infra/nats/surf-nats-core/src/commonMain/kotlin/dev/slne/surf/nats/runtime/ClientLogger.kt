package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

/**
 * Logger of one client: SLF4J (`ComponentLogger`) on the JVM, `console` on JS.
 */
@InternalNatsApi
internal expect class ClientLogger(name: String) {

    fun debug(message: String, cause: Throwable? = null)

    fun info(message: String, cause: Throwable? = null)

    fun warn(message: String, cause: Throwable? = null)

    fun error(message: String, cause: Throwable? = null)
}
