package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
internal actual class ClientLogger actual constructor(name: String) {
    private val prefix = "$name:"

    actual fun debug(message: String, cause: Throwable?) {
        if (cause == null) console.log(prefix, message) else console.log(prefix, message, cause)
    }

    actual fun info(message: String, cause: Throwable?) {
        if (cause == null) console.info(prefix, message) else console.info(prefix, message, cause)
    }

    actual fun warn(message: String, cause: Throwable?) = warn(message, cause as Any?)

    actual fun error(message: String, cause: Throwable?) = error(message, cause as Any?)

    /**
     * Logs [detail] as is, e.g. a promise rejection value that is no [Throwable].
     */
    fun warn(message: String, detail: Any?) {
        if (detail == null) console.warn(prefix, message) else console.warn(prefix, message, detail)
    }

    /**
     * Logs [detail] as is, e.g. a promise rejection value that is no [Throwable].
     */
    fun error(message: String, detail: Any?) {
        if (detail == null) console.error(prefix, message) else console.error(prefix, message, detail)
    }
}
