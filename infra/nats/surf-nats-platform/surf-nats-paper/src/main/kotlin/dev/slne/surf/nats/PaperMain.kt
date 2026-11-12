package dev.slne.surf.nats

import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

/**
 * Disconnects NATS when the plugin is disabled, blocking the calling thread until the connection is drained.
 */
@InternalNatsApi
class PaperMain : JavaPlugin() {

    override fun onDisable() {
        runBlocking {
            NatsInstance.get().disable()
        }
    }
}
