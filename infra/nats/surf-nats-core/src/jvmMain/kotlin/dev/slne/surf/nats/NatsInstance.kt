package dev.slne.surf.nats

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.nats.client.NatsClientFactoryImpl
import dev.slne.surf.nats.connection.NatsConnectionHealth
import dev.slne.surf.nats.connection.NatsConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.MustBeInvokedByOverriders
import java.nio.file.Path

@InternalNatsApi
abstract class NatsInstance {

    abstract val dataPath: Path

    @MustBeInvokedByOverriders
    open suspend fun load() {
        withContext(Dispatchers.IO) {
            NatsConnectionManager.connect()
        }
    }

    /**
     * Drains the registrations of all clients so no new events or requests arrive; publishing and service calls keep
     * working until [disable].
     */
    @MustBeInvokedByOverriders
    open suspend fun stopAccepting() {
        withContext(Dispatchers.IO) {
            NatsClientFactoryImpl.get().stopAcceptingAll()
        }
    }

    @MustBeInvokedByOverriders
    open suspend fun disable() {
        NatsClientFactoryImpl.get().ensureAllClientsClosed()
        withContext(Dispatchers.IO) {
            NatsConnectionManager.disconnect()
        }
    }

    fun connectionHealth(): NatsConnectionHealth? = NatsConnectionManager.health()

    companion object {
        val instance = requiredService<NatsInstance>()
        fun get() = instance
    }
}