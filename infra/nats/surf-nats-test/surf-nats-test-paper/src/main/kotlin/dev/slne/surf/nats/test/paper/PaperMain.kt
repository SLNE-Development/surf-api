package dev.slne.surf.nats.test.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.nats.test.common.NatsTestInstance
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class PaperMain : SuspendingJavaPlugin() {
    val origin get() = "paper:${Bukkit.getPort()}"

    override suspend fun onLoadAsync() {
        NatsTestInstance.get().load()
        registerNatsTestCommand()
    }

    override suspend fun onEnableAsync() {
        NatsTestInstance.get().enable()
    }

    override suspend fun onDisableAsync() {
        NatsTestInstance.get().disable()
    }
}

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)
