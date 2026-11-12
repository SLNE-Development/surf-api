package dev.slne.surf.nats.client

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.internal.NatsClientFactory
import dev.slne.surf.nats.protocol.ProtocolNaming
import kotlinx.coroutines.delay
import org.jetbrains.annotations.Blocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Creates the `NatsClient`s of this process and tracks the open ones by name.
 *
 * A name is free again once its client is closed. Thread-safe.
 */
@InternalNatsApi
internal class NatsClientFactoryImpl : NatsClientFactory {
    private val clients = ConcurrentHashMap<String, NatsClientImpl>()

    @Volatile
    private var accepting = true

    /**
     * Creates a client named [name].
     *
     * Throws [IllegalArgumentException] when [name] is not a valid client name and [IllegalStateException] when an
     * open client already has that name.
     */
    override fun create(name: String): NatsClient {
        ProtocolNaming.requireClientName(name)

        val origin = ClientOrigin.capture()
        val client = clients.compute(name) { _, existing ->
            check(existing == null) { "NATS client '$name' already exists" }
            NatsClientImpl(name, origin, onClose = { clients.remove(name, it) })
        }!!

        if (!accepting) {
            client.stopAccepting()
        }

        return client
    }

    /**
     * Stops inbound work of every open client, see [NatsClientImpl.stopAccepting], and blocks until all drains
     * complete. Clients created afterwards start without accepting registrations.
     */
    @Blocking
    fun stopAcceptingAll() {
        accepting = false
        for (client in clients.values) {
            client.stopAccepting()
        }
    }

    /**
     * Warns about clients that are still open, waits 10 seconds and logs the creation site of each one still open
     * then. Closes no client.
     */
    suspend fun ensureAllClientsClosed() {
        val open = clients.values.toList()
        if (open.isEmpty()) return

        log.atWarning().log(
            "NATS is being disconnected while %s NatsClient(s) are still open. " +
                    "These plugins probably did not call NatsClient.close(): %s",
            open.size,
            open.joinToString { "${it.name} from ${it.clientOrigin.source}" }
        )

        log.atWarning().log(
            "Any NATS errors that appear after this message are expected follow-up errors caused by " +
                    "disconnecting while clients are still open. Fix the plugins listed above by closing " +
                    "their NatsClient during shutdown."
        )

        delay(10.seconds)

        for (client in open.filterNot { it.isClosed }) {
            val origin = client.clientOrigin
            log.atWarning()
                .log(
                    """
                        Unclosed NatsClient:
                        name: ${client.name}
                        source: ${origin.source}
                        createdAt: ${origin.createdAt}
                        creationThread: ${origin.thread}
                        creationStackTrace:
                        """.trimIndent() + origin.stackTrace.joinToString("") { "\n    at $it" }
                )
        }
    }

    companion object {
        private val log = logger()
        fun get() = NatsClientFactory.instance as NatsClientFactoryImpl
    }
}