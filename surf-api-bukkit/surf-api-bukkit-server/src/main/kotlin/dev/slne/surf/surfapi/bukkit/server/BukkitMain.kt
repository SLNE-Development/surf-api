package dev.slne.surf.surfapi.bukkit.server

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.surfapi.bukkit.server.libs.LibLoader
import org.bukkit.plugin.java.JavaPlugin

class BukkitMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        LibLoader(classLoader).loadLibs()
        BukkitInstance.onLoad()
    }

    override suspend fun onEnableAsync() {
        BukkitInstance.onEnable()
    }

    override suspend fun onDisableAsync() {
        BukkitInstance.onDisable()
    }
}

val plugin = JavaPlugin.getPlugin(BukkitMain::class.java)
