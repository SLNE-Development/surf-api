package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.presenceKind
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ProtocolHeaders
import io.nats.client.Dispatcher
import kotlinx.coroutines.*
import java.lang.AutoCloseable

@InternalNatsApi
internal class PresenceAnnouncer(
    private val client: NatsClientImpl,
    private val subject: String,
) : AutoCloseable {

    private val lock = Any()

    @Volatile
    private var closed = false

    @Volatile
    private var heartbeat: Job? = null

    fun start(dispatcher: Dispatcher) {
        dispatcher.subscribe(subject) { message ->
            if (message.presenceKind() == ProtocolHeaders.PRESENCE_QUERY) {
                beat()
            }
        }
        synchronized(lock) {
            if (closed) return
            client.publishPresence(subject, ProtocolHeaders.PRESENCE_JOIN)
        }
        heartbeat = client.clientScope.launch(Dispatchers.IO + CoroutineName("surf-nats/${client.name}/presence")) {
            while (isActive && !closed) {
                delay(RpcDefaults.HEARTBEAT_INTERVAL)
                beat()
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            client.publishPresence(subject, ProtocolHeaders.PRESENCE_LEAVE)
        }
        heartbeat?.cancel("Presence announcer closed")
    }

    private fun beat() = synchronized(lock) {
        if (!closed) client.publishPresence(subject, ProtocolHeaders.PRESENCE_BEAT)
    }
}

internal fun NatsClientImpl.publishPresence(subject: String, kind: String) {
    try {
        val presenceHeaders = headers().add(ProtocolHeaders.PRESENCE, kind)
        NatsConnectionManager.connection().publish(subject, presenceHeaders, NatsHeaders.EMPTY_BODY)
    } catch (e: Exception) {
        logger.debug("Failed to publish presence '$kind' on $subject", e)
    }
}
