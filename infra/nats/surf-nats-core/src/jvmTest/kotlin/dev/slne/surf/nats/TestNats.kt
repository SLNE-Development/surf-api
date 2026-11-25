package dev.slne.surf.nats

import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.embedded.EmbeddedNats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

/**
 * Connects the shared NATS connection of this JVM to the test server.
 *
 * Installs a test `config.yml`, which also sets the handler limits of the limited test services.
 */
object TestNats {

    private val configDirectory: Path by lazy { createConfigDirectory() }

    private fun createConfigDirectory(): Path {
        val directory = Files.createTempDirectory("surf-nats-test")
        val config = """
            |servers:
            |  - "${TestNatsServer.url}"
            |max-concurrent-handlers-by-service:
            |  "${LimitedService.NATS_DESCRIPTOR.protoName}": 1
            |  "${LimitedPersistentService.NATS_DESCRIPTOR.protoName}": 1
            |  "${LaneService.NATS_DESCRIPTOR.protoName}": 2
            |max-concurrent-long-handlers-by-service:
            |  "${LaneService.NATS_DESCRIPTOR.protoName}": 1
            |""".trimMargin()
        directory.resolve("config.yml").writeText(config)

        return directory
    }

    /**
     * Installs the test `config.yml` as the NATS configuration directory. Does nothing when already
     * installed.
     */
    fun installConfiguration() {
        EmbeddedNats.useConfigDirectory(configDirectory)
    }

    /**
     * Connects to the test server. Does nothing when already connected.
     */
    suspend fun load() {
        installConfiguration()
        withContext(Dispatchers.IO) {
            NatsConnectionManager.connect()
        }
    }

    /**
     * Drains and closes the connection.
     */
    suspend fun disable() {
        withContext(Dispatchers.IO) {
            NatsConnectionManager.disconnect()
        }
    }
}
