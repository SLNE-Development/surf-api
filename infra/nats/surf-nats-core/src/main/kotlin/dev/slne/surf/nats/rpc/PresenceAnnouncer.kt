package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.WireFormat
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

    @Volatile
    private var heartbeat: Job? = null

    fun start(dispatcher: Dispatcher) {
        dispatcher.subscribe(subject) { message ->
            if (message.presenceKind() == NatsHeaders.PRESENCE_QUERY) {
                announce(NatsHeaders.PRESENCE_BEAT)
            }
        }
        announce(NatsHeaders.PRESENCE_JOIN)
        heartbeat = client.clientScope.launch(CoroutineName("surf-nats/${client.name}/presence")) {
            while (isActive) {
                delay(RpcDefaults.HEARTBEAT_INTERVAL)
                announce(NatsHeaders.PRESENCE_BEAT)
            }
        }
    }

    override fun close() {
        heartbeat?.cancel("Presence announcer closed")
        announce(NatsHeaders.PRESENCE_LEAVE)
    }

    private fun announce(kind: String) {
        try {
            val headers = NatsHeaders.base(client.name, WireFormat.CBOR)
                .add(NatsHeaders.PRESENCE, kind)
            NatsConnectionManager.connection().publish(subject, headers, EMPTY)
        } catch (e: Exception) {
            client.logger.debug("Failed to announce presence '$kind' on $subject", e)
        }
    }

    companion object {
        private val EMPTY = ByteArray(0)
    }

}