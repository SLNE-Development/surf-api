package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.nats.SurfNatsStandalone
import io.nats.client.Connection
import io.nats.client.Nats
import java.nio.file.Files
import kotlin.io.path.writeText

/**
 * Connects surf-nats in this JVM to the test NATS container, as the microservice runtime does.
 */
object TestNats {

    /**
     * Loads surf-nats; pair every call with [stop].
     */
    suspend fun start() {
        val directory = Files.createTempDirectory("surf-reliability-nats")
        directory.resolve("config.yml").writeText("servers:\n  - \"${Containers.nats.connectionUrl}\"\n")
        SurfNatsStandalone.start(directory)
    }

    suspend fun stop() {
        SurfNatsStandalone.shutdown()
    }

    /**
     * A plain jnats connection to the test container, for operator actions such as a dead-letter replay.
     */
    fun rawConnection(): Connection = Nats.connect(Containers.nats.connectionUrl)

    /**
     * Number of messages stored in [stream].
     */
    fun Connection.streamMessageCount(stream: String): Long =
        jetStreamManagement().getStreamInfo(stream).streamState.msgCount
}
