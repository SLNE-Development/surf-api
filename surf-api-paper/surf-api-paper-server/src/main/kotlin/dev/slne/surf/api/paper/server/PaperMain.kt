package dev.slne.surf.api.paper.server

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.paper.server.impl.scoreboard.SurfScoreboardApiImpl
import dev.slne.surf.api.paper.server.libs.LibLoader
import org.bukkit.plugin.java.JavaPlugin

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        LibLoader(classLoader).loadLibs()
        PaperInstance.onLoad()
    }

    override suspend fun onEnableAsync() {
        PaperInstance.onEnable()
        SurfScoreboardApiImpl.INSTANCE.onEnable()
    }

    override suspend fun onDisableAsync() {
        SurfScoreboardApiImpl.INSTANCE.onDisable()
        PaperInstance.onDisable()
    }
}

val plugin = JavaPlugin.getPlugin(PaperMain::class.java)
