package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolDefaults

@InternalNatsApi
internal abstract class AbstractNatsClient<R : Any>(val name: String) {
    protected val lifecycle = ClientLifecycle(name)

    val logger = ClientLogger(lifecycle.label)

    val isClosed: Boolean get() = lifecycle.isClosed

    private val registrations = concurrentSetOf<R>()

    fun checkOpen() = lifecycle.requireOpen()

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

    protected abstract fun closeRegistration(registration: R)

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
     * Requires an event of [size] bytes including headers to fit into [maxPayload]; events are never offloaded.
     */
    protected fun requireEventFits(typeName: String, subject: String, size: Int, maxPayload: Long) {
        require(size <= maxPayload) { ProtocolDefaults.eventTooLargeMessage(typeName, size, maxPayload, subject) }
    }
}
