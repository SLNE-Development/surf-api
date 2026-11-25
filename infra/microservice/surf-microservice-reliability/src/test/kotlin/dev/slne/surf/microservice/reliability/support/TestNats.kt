@file:OptIn(InternalNatsApi::class)

package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.embedded.EmbeddedNats
import io.nats.client.Connection
import io.nats.client.Nats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

/**
 * Connects surf-nats in this JVM to the test NATS container, as the surf-api runtime does.
 */
object TestNats {

    /**
     * The configuration directory is installed once per JVM, so every test class shares this one.
     */
    private val configDirectory: Path by lazy { createConfigDirectory() }

    private var connectionHolder: NatsClient? = null

    private fun createConfigDirectory(): Path {
        val directory = Files.createTempDirectory("surf-reliability-nats")
        directory.resolve("config.yml")
            .writeText("servers:\n  - \"${Containers.nats.connectionUrl}\"\n")

        return directory
    }

    /**
     * Opens the shared connection through a client that holds it; pair every call with [stop].
     */
    suspend fun start() {
        check(connectionHolder == null) { "TestNats is already started" }

        EmbeddedNats.useConfigDirectory(configDirectory)
        val holder = NatsClient.create(TestClients.uniqueName("connection-holder"))

        try {
            withContext(Dispatchers.IO) { EmbeddedNats.control(holder).connect() }
        } catch (e: Throwable) {
            holder.close()
            throw e
        }

        connectionHolder = holder
    }

    /**
     * Closes the holder, which closes the connection once no other client holds it.
     */
    suspend fun stop() {
        val holder = checkNotNull(connectionHolder) { "TestNats is not started" }
        connectionHolder = null
        withContext(Dispatchers.IO) { holder.close() }
    }

    /**
     * A plain jnats connection to the test container, for operator actions such as a dead-letter
     * replay.
     */
    fun rawConnection(): Connection = Nats.connect(Containers.nats.connectionUrl)

    /**
     * Number of messages stored in [stream].
     */
    fun Connection.streamMessageCount(stream: String): Long {
        return jetStreamManagement().getStreamInfo(stream).streamState.msgCount
    }
}
