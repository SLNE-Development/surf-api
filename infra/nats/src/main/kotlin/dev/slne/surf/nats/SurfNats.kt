package dev.slne.surf.nats

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.standalone.SurfApiStandaloneBootstrap
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    // fire and forget
    val dispatcher = connection.createDispatcher { message ->
        val decodedMessage = Cbor.decodeFromByteArray<PubSubMessage>(message.data)
        log.atInfo().log("Received message: %s", decodedMessage)
    }.subscribe("surf.test.pubsub")

    connection.publish("surf.test.pubsub", Cbor.encodeToByteArray(PubSubMessage("Hello, NATS!")))

    // request-reply
    val requestReplayDispatcher = connection.createDispatcher { message ->
        val replyTo = message.replyTo ?: return@createDispatcher
        val decodedRequest = Cbor.decodeFromByteArray<PubSubRequestMessage>(message.data)
        log.atInfo().log("Received request: %s", decodedRequest)
        connection.publish(
            replyTo,
            Cbor.encodeToByteArray(PubSubReplyMessage("Reply to: ${decodedRequest.message}"))
        )
    }.subscribe("surf.test.request.reply")

    val request = connection.request(
        "surf.test.request.reply",
        Cbor.encodeToByteArray(PubSubRequestMessage("Hello, NATS!"))
    )

    val response = withTimeoutOrNull(1.seconds) { request.await() }
    if (response == null) {
        log.atWarning().log("Request timed out")
    } else {
        val decodedResponse = Cbor.decodeFromByteArray<PubSubReplyMessage>(response.data)
        log.atInfo().log("Received response: %s", decodedResponse)
    }

    connection.flush(Duration.ofSeconds(1))
    connection.closeDispatcher(dispatcher)
    connection.closeDispatcher(requestReplayDispatcher)
}

@Serializable
data class PubSubMessage(
    val message: String
)

@Serializable
data class PubSubRequestMessage(
    val message: String
)

@Serializable
data class PubSubReplyMessage(
    val message: String
)
