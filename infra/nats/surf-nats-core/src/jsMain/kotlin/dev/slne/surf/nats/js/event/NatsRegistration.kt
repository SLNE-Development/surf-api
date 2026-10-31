@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.client.NatsClientImpl

@JsExport
class NatsRegistration internal constructor(
    private val client: NatsClientImpl,
    private val listener: EventListener,
) {
    private var active = true
    val isClosed: Boolean get() = !active

    fun close() {
        if (!active) return
        active = false
        try {
            client.eventSubscriptions.remove(listener)
        } catch (e: Throwable) {
            client.logger.warn("Failed to remove the listener for ${listener.descriptor.typeName} on ${listener.descriptor.subject}", e)
        } finally {
            client.unregisterRegistration(this)
        }
    }
}
