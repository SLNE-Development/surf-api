package dev.slne.surf.nats

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.standalone.SurfApiStandaloneBootstrap
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.time.Duration

private val log = logger()

suspend fun main() {
    SurfApiStandaloneBootstrap.bootstrap()
    SurfApiStandaloneBootstrap.enable()

    try {
        val options = Options.builder()
            .server(System.getenv("NATS_URL") ?: Options.DEFAULT_URL)
            .connectionName("surf-nats")
            .build()

        Nats.connect(options).use { connection ->
            log.atInfo().log("Connected to %s", connection.connectedUrl)
            playground(connection)
        }
    } finally {
        SurfApiStandaloneBootstrap.shutdown()
    }
}

@OptIn(ExperimentalSerializationApi::class)
private suspend fun playground(connection: Connection) {
    val dispatcher = connection.createDispatcher { message ->
        val decodedMessage = Cbor.decodeFromByteArray<PubSubMessage>(message.data)
        log.atInfo().log("Received message: %s", decodedMessage)
    }.subscribe("surf.test.pubsub")

    connection.publish("surf.test.pubsub", Cbor.encodeToByteArray(PubSubMessage("Hello, NATS!")))

    connection.flush(Duration.ofSeconds(1))
    connection.closeDispatcher(dispatcher)
}

@Serializable
data class PubSubMessage(
    val message: String
)
