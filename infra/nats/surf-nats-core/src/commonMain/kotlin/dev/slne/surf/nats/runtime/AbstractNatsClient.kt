package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolDefaults

/**
 * Lifecycle, logging and registration tracking shared by the JVM and the JS client.
 *
 * [R] is the registration type of the platform client. Registrations are admitted only while the client accepts new
 * work, see [ClientLifecycle.admit]. Thread-safe.
 */
@InternalNatsApi
internal abstract class AbstractNatsClient<R : Any>(val name: String) {
    protected val lifecycle = ClientLifecycle(name)

    val logger = ClientLogger(lifecycle.label)

    val isClosed: Boolean get() = lifecycle.isClosed

    private val registrations = concurrentSetOf<R>()

    fun checkOpen() = lifecycle.requireOpen()

    /**
     * Tracks [registration] until it is unregistered or the client closes it.
     *
     * Throws [IllegalStateException] when the client is closing or closed. The registration is then closed through
     * [closeRegistration].
     */
    fun registerRegistration(registration: R) {
        lifecycle.admit(
            add = { registrations.add(registration) },
            undo = {
                registrations.remove(registration)
                closeRegistration(registration)
            },
        )
    }

    fun unregisterRegistration(registration: R) {
        registrations.remove(registration)
    }

    /**
     * Closes [registration]. Implementations remove it through [unregisterRegistration].
     */
    protected abstract fun closeRegistration(registration: R)

    /**
     * Closes every tracked registration. Failures are logged and do not stop the remaining closes.
     */
    protected fun closeRegistrations() {
        for (registration in registrations.toList()) {
            try {
                closeRegistration(registration)
            } catch (e: Exception) {
                logger.error("Failed to close registration $registration", e)
            }
        }

        if (registrations.isNotEmpty()) {
            logger.warn("Some registrations were not closed properly: ${registrations.size} remaining")
        }
    }

    /**
     * Throws [IllegalArgumentException] when an event of [size] bytes including headers exceeds [maxPayload].
     *
     * Events are never offloaded.
     */
    protected fun requireEventFits(typeName: String, subject: String, size: Int, maxPayload: Long) {
        require(size <= maxPayload) { ProtocolDefaults.eventTooLargeMessage(typeName, size, maxPayload, subject) }
    }
}
