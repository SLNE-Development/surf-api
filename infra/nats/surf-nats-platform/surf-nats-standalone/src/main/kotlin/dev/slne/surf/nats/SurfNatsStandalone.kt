package dev.slne.surf.nats

import java.nio.file.Path

/**
 * Controls the shared NATS connection of a standalone JVM application.
 *
 * Call [start] before creating a [NatsClient] and close every client before [shutdown]. A microservice built on
 * surf-microservice does not call these functions: the bundled microservice integration calls [start] with the
 * `nats` directory below the microservice data path before `Microservice.onBootstrap`, [stopAccepting] before
 * `Microservice.onDisable` and [shutdown] after it.
 *
 * ```kotlin
 * suspend fun main() {
 *     SurfNatsStandalone.start(Path.of("nats"))
 *     val client = NatsClient.create("my-app")
 *     try {
 *         // register listeners and services, publish events, call services
 *     } finally {
 *         client.close()
 *         SurfNatsStandalone.shutdown()
 *     }
 * }
 * ```
 */
object SurfNatsStandalone {

    /**
     * Connects to NATS with the configuration in `<dataPath>/config.yml`.
     *
     * Suspends until the connection is established. Does nothing when already connected. The configuration is read
     * on the first call and kept for the lifetime of the JVM, so a later call with a different [dataPath] reuses it.
     *
     * @param dataPath the directory that holds `config.yml`.
     * @throws java.io.IOException if no configured server accepts the connection.
     * @throws IllegalStateException if the configuration is invalid.
     */
    suspend fun start(dataPath: Path) {
        InstanceImpl.dataPath = dataPath
        NatsInstance.instance.load()
    }

    /**
     * Stops all clients from receiving events and requests while publishing and service calls keep working until
     * [shutdown].
     *
     * Suspends until the inbound registrations of every open client are closed and drained. Afterwards
     * `registerListener` and `registerService` throw [IllegalStateException] on every client, including clients
     * created later in the same JVM, also after another [start]. Call it only as the first step of the final shutdown.
     */
    suspend fun stopAccepting() {
        NatsInstance.instance.stopAccepting()
    }

    /**
     * Drains and closes the NATS connection.
     *
     * Close every [NatsClient] first. When clients are still open, logs a warning with their creation sites and waits
     * 10 seconds before disconnecting. A client never survives a shutdown, so create new clients after the next
     * [start].
     */
    suspend fun shutdown() {
        NatsInstance.instance.disable()
    }
}
