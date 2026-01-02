package dev.slne.surf.redis

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import org.bukkit.plugin.java.JavaPlugin

class PaperMain : SuspendingJavaPlugin() {
    override fun onDisable() {
        RedisInstance.instance.disable()
    }
}

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)