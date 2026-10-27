@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.external.nats.core.Subscription
import kotlin.js.Promise

@JsExport
class NatsRegistration internal constructor(
    private val listeners: EventListeners,
    private val subscription: Subscription,
) {
    private var closed = false
    val isClosed: Boolean get() = closed

    fun close() {
        if (!markClosed()) return
        if (!subscription.isClosed()) subscription.unsubscribe()
    }

    /**
     * Stops the delivery like [close]; messages already received are still dispatched before the promise resolves.
     */
    internal fun drain(): Promise<Unit>? {
        if (!markClosed()) return null
        return if (subscription.isClosed()) null else subscription.drain()
    }

    private fun markClosed(): Boolean {
        if (closed) return false
        closed = true
        listeners.unregister(this)
        return true
    }
}
