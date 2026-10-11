package dev.slne.surf.nats

import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

@InternalNatsApi
class PaperMain : JavaPlugin() {

    override fun onDisable() {
        runBlocking {
            NatsInstance.get().disable()
        }
    }
}
