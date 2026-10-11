package dev.slne.surf.nats.client

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.internal.NATS_CLIENT_NAME_PATTERN
import dev.slne.surf.nats.internal.NatsClientFactory
import kotlinx.coroutines.delay
import kotlinx.serialization.modules.SerializersModule
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@AutoService(NatsClientFactory::class)
@InternalNatsApi
internal class NatsClientFactoryImpl : NatsClientFactory {
    private val namePattern = NATS_CLIENT_NAME_PATTERN.toRegex()

    private val clients = ConcurrentHashMap<String, NatsClientImpl>()

    override fun create(
        name: String,
        serializersModule: SerializersModule
    ): NatsClient {
        require(namePattern.matches(name)) {
            "Invalid NATS client name '$name': must be kebab-case ([a-z0-9] separated by '-')"
        }

        val origin = ClientOrigin.capture()
        return clients.compute(name) { _, existing ->
            check(existing == null) { "NATS client '$name' already exists" }
            NatsClientImpl(name, serializersModule, origin, onClose = { clients.remove(name, it) })
        }!!
    }

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