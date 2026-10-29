package dev.slne.surf.nats.js.client

/**
 * Console logger of one client, prefixed like the `NatsClient[<name>]` logger of the JVM client.
 */
internal class ClientLogger(name: String) {
    private val prefix = "NatsClient[$name]:"

    fun info(message: String) {
        console.info(prefix, message)
    }

    fun warn(message: String, error: Any? = null) {
        if (error == null) {
            console.warn(prefix, message)
        } else {
            console.warn(prefix, message, error)
        }
    }

    fun error(message: String, error: Any? = null) {
        if (error == null) {
            console.error(prefix, message)
        } else {
            console.error(prefix, message, error)
        }
    }
}
