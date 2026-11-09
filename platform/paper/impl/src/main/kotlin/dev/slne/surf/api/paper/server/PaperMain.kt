package dev.slne.surf.api.paper.server

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.di.boot.SurfContainerAccess
import dev.slne.surf.api.di.boot.SurfContainerHost
import dev.slne.surf.api.paper.api.metrics.Metrics
import dev.slne.surf.api.paper.server.impl.di.PaperContainerPlatform
import dev.slne.surf.api.paper.server.impl.scoreboard.SurfScoreboardApiImpl
import dev.slne.surf.api.paper.server.impl.sidebar.SidebarRegistry
import dev.slne.surf.api.paper.server.libs.LibLoader
import org.bukkit.plugin.java.JavaPlugin

class PaperMain : SuspendingJavaPlugin() {

    lateinit var bstats: Metrics

    /**
     * surf-api's container hierarchy for this server.
     *
     * The root holds surf-api's own beans; each plugin that wants one gets a child below it. There
     * is one host per process, and this is it.
     */
    val containers = SurfContainerHost(PaperContainerPlatform())

    override suspend fun onLoadAsync() {
        LibLoader(classLoader).loadLibs()
        bstats = Metrics(this, 29465)

        // Before anything else in surf-api loads. A bean is resolved where it is first used rather
        // than where it is declared, so the container has to be able to answer from the first class
        // that asks — and `install` is what makes it answerable through `SurfContainer`.
        containers.startRoot(this)
        SurfContainerAccess.install(containers)

        PaperInstance.onLoad()
    }

    override suspend fun onEnableAsync() {
        PaperInstance.onEnable()
        SurfScoreboardApiImpl.INSTANCE.onEnable()
    }

    override suspend fun onDisableAsync() {
        if (::bstats.isInitialized) {
            bstats.shutdown()
        }

        SidebarRegistry.closeAll()
        SurfScoreboardApiImpl.INSTANCE.onDisable()
        PaperInstance.onDisable()

        // Last, and children before the root: a plugin's bean may hold one of surf-api's, so the
        // reverse order would close a connection while something still writes to it.
        containers.stopAll()
        SurfContainerAccess.uninstall(containers)
    }
}

val plugin = JavaPlugin.getPlugin(PaperMain::class.java)
