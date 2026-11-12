package dev.slne.surf.nats

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.nats.client.NatsClientFactoryImpl
import dev.slne.surf.nats.connection.NatsConnectionHealth
import dev.slne.surf.nats.connection.NatsConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.MustBeInvokedByOverriders
import java.nio.file.Path

/**
 * Owns the process-wide NATS connection and drives its lifecycle for a platform module.
 *
 * Each platform module provides one implementation as a service, available through [instance]. The platform calls
 * [load] at startup, optionally [stopAccepting] at the start of shutdown, and [disable] at the end of shutdown. All
 * `NatsClient`s share the connection.
 *
 * A `NatsClient` does not survive [disable]. After a later [load], create new clients. After [stopAccepting], new
 * clients accept no registrations for the rest of the process.
 */
@InternalNatsApi
abstract class NatsInstance {

    /**
     * The directory that holds the NATS `config.yml`.
     */
    abstract val dataPath: Path

    /**
     * Connects to NATS.
     *
     * The first call reads and validates the configuration, later calls reuse it. Returns immediately when the
     * connection already exists. Throws when the configuration is invalid or the connection cannot be established.
     */
    @MustBeInvokedByOverriders
    open suspend fun load() {
        withContext(Dispatchers.IO) {
            NatsConnectionManager.connect()
        }
    }

    /**
     * Drains the registrations of all clients so no new events or requests arrive.
     *
     * Returns once the handlers that were running have finished or were cancelled after the drain timeout. Clients
     * created afterwards accept no registrations. Publishing and service calls keep working until [disable].
     */
    @MustBeInvokedByOverriders
    open suspend fun stopAccepting() {
        withContext(Dispatchers.IO) {
            NatsClientFactoryImpl.get().stopAcceptingAll()
        }
    }

    /**
     * Drains and closes the NATS connection.
     *
     * Clients still open at this point are not closed. They are logged with their creation site, and the call waits
     * 10 seconds before the connection is drained. Close every client before calling this function.
     */
    @MustBeInvokedByOverriders
    open suspend fun disable() {
        NatsClientFactoryImpl.get().ensureAllClientsClosed()
        withContext(Dispatchers.IO) {
            NatsConnectionManager.disconnect()
        }
    }

    /**
     * Returns a snapshot of the connection state, or `null` before [load] and after [disable].
     *
     * Reads the state only and never contacts the server.
     */
    fun connectionHealth(): NatsConnectionHealth? = NatsConnectionManager.health()

    companion object {
        /**
         * The implementation provided by the platform module.
         */
        val instance = requiredService<NatsInstance>()

        /**
         * Returns [instance].
         */
        fun get() = instance
    }
}