package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.api.EventContext
import dev.slne.surf.nats.js.client.JsRuntimeDefaults
import dev.slne.surf.nats.js.client.NatsClientImpl
import dev.slne.surf.nats.js.codec.NatsCodec
import dev.slne.surf.nats.js.codec.header
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.Subscription
import dev.slne.surf.nats.js.external.nats.core.SubscriptionOptions
import dev.slne.surf.nats.protocol.ProtocolHeaders
import kotlinx.coroutines.await

/**
 * The subscriptions of one client: one per subject and queue group, shared by every listener registered for it.
 */
internal class EventSubscriptions(private val client: NatsClientImpl) {

    private data class Key(val subject: String, val queueGroup: String?)

    private class Entry(
        val key: Key,
        val descriptor: EventDescriptor,
        var targets: List<EventListener>,
    ) {
        lateinit var subscription: Subscription
    }

    private val entries = HashMap<Key, Entry>()
    private val runningHandlers = RunningHandlers()
    private var closed = false

    fun add(listener: EventListener) {
        check(!closed) { "NatsClient[${client.name}] is closed" }

        val key = keyOf(listener)

        for ((otherKey, other) in entries) {
            if (otherKey.subject != key.subject) continue
            check(other.descriptor.schema === listener.descriptor.schema) {
                "Subject '${key.subject}' is already used by ${other.descriptor.typeName}, " +
                        "cannot register a listener for ${listener.descriptor.typeName}"
            }
        }

        // nats.js closes a subscription it reports an error for; the next listener subscribes again
        val existing = entries[key]
        if (existing != null && !existing.subscription.isClosed()) {
            existing.targets += listener
            return
        }

        val entry = Entry(key, listener.descriptor, listOf(listener))
        entry.subscription = client.connection.subscribe(
            key.subject,
            SubscriptionOptions(
                queue = key.queueGroup,
                callback = { error, message -> dispatch(entry, error, message) },
            ),
        )

        entries[key] = entry
    }

    fun remove(listener: EventListener) {
        val key = keyOf(listener)
        val entry = entries[key] ?: return

        entry.targets = entry.targets.filter { it !== listener }

        if (entry.targets.isEmpty()) {
            entries.remove(key)
            if (!entry.subscription.isClosed()) entry.subscription.unsubscribe()
        }
    }

    /**
     * Stops every subscription after the messages it already received are dispatched, then waits for the running
     * handlers up to [JsRuntimeDefaults.drainTimeout].
     */
    suspend fun drain() {
        if (closed) return
        closed = true

        val drained = entries.values.toList()
        entries.clear()

        val drains =
            drained.filterNot { it.subscription.isClosed() }.map { it.subscription.drain() }
        for (drain in drains) {
            try {
                drain.await()
            } catch (e: Throwable) {
                client.logger.warn("Failed to drain a listener subscription", e)
            }
        }

        val timeout = JsRuntimeDefaults.drainTimeout
        if (!runningHandlers.awaitIdle(timeout)) {
            client.logger.warn(
                "${runningHandlers.count} listener handlers are still running after $timeout, closing anyway"
            )
        }
    }

    private fun dispatch(entry: Entry, error: Throwable?, message: Msg) {
        val descriptor = entry.descriptor
        if (error != null) {
            client.logger.warn(
                "Listener subscription for ${descriptor.typeName} on ${entry.key.subject} stopped",
                error
            )
            return
        }

        val origin = message.header(ProtocolHeaders.ORIGIN)
        val contentType = message.header(ProtocolHeaders.CONTENT_TYPE)

        val fromThisClient = origin == client.origin
        val targets =
            entry.targets // a handler may close its registration while the targets are dispatched

        if (fromThisClient && targets.all { it.ignoreFromThisClient }) return

        val event = try {
            NatsCodec.decode(
                descriptor.schema,
                message.data,
                contentType,
                "event on ${message.subject}"
            )
        } catch (e: Throwable) {
            client.logger.warn(
                "Dropping undecodable ${descriptor.typeName} on ${message.subject}",
                e
            )
            return
        }

        val context = EventContext(origin = origin, isFromThisClient = fromThisClient)

        for (target in targets) {
            if (fromThisClient && target.ignoreFromThisClient) continue
            val result = try {
                target.handler(event, context)
            } catch (e: Throwable) {
                logListenerFailure(descriptor, message, e)
                continue
            }
            runningHandlers.track(result) { logListenerFailure(descriptor, message, it) }
        }
    }

    private fun logListenerFailure(descriptor: EventDescriptor, message: Msg, error: Any?) {
        client.logger.error(
            "Listener for ${descriptor.typeName} failed on ${message.subject}",
            error
        )
    }

    private fun keyOf(listener: EventListener): Key = Key(listener.descriptor.subject, listener.queueGroup)
}
