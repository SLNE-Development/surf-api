package dev.slne.surf.nats

import dev.slne.surf.api.core.util.requiredService
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

    @MustBeInvokedByOverriders
    open suspend fun disable() {
        withContext(Dispatchers.IO) {
            NatsConnectionManager.disconnect()
        }
    }

    companion object {
        val instance = requiredService<NatsInstance>()
        fun get() = instance
    }
}