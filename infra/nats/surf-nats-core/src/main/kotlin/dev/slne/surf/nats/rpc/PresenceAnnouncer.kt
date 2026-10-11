package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.presenceKind
import dev.slne.surf.nats.connection.NatsConnectionManager
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
            if (message.presenceKind() == NatsHeaders.PRESENCE_QUERY) {
                beat()
            }
        }
        synchronized(lock) {
            if (closed) return
            announce(NatsHeaders.PRESENCE_JOIN)
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
            announce(NatsHeaders.PRESENCE_LEAVE)
        }
        heartbeat?.cancel("Presence announcer closed")
    }

    private fun beat() = synchronized(lock) {
        if (!closed) announce(NatsHeaders.PRESENCE_BEAT)
    }

    private fun announce(kind: String) {
        try {
            NatsConnectionManager.connection().publish(subject, NatsHeaders.presence(client.name, kind), NatsHeaders.EMPTY_BODY)
        } catch (e: Exception) {
            client.logger.debug("Failed to announce presence '$kind' on $subject", e)
        }
    }
}
