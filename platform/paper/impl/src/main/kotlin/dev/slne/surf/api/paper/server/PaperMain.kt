package dev.slne.surf.api.paper.server

import dev.slne.surf.api.paper.api.metrics.Metrics
import dev.slne.surf.api.paper.plugin.SurfPaperPluginRuntime
import dev.slne.surf.api.paper.server.impl.sidebar.SidebarRegistry
import org.bukkit.plugin.java.JavaPlugin

class PaperMain : JavaPlugin() {

    lateinit var bstats: Metrics

    private val runtime get() = PaperBoostrapper.springRuntime

    override fun onLoad() {
        runtime.attachRootPlugin(this)
        runtime.runRootPhase {
            bstats = Metrics(this, BSTATS_PLUGIN_ID)

            runtime.loadRoot()
            PaperInstance.onLoad()
        }
    }

    override fun onEnable() {
        runtime.runRootPhase {
            runtime.enableRoot()
            PaperInstance.onEnable()
        }
    }

    override fun onDisable() {
        runtime.runRootPhase {
            runtime.beginRootDisable()

            try {
                runtime.closePlugins()
            } finally {
                SurfPaperPluginRuntime.uninstall()
            }

            try {
                tearDown()
            } finally {
                runtime.shutdownRoot()
            }
        }
    }

    private suspend fun tearDown() {
        if (::bstats.isInitialized) {
            bstats.shutdown()
        }

        SidebarRegistry.closeAll()
        PaperInstance.onDisable()
    }

    private companion object {
        const val BSTATS_PLUGIN_ID = 29465
    }
}

val plugin = JavaPlugin.getPlugin(PaperMain::class.java)
