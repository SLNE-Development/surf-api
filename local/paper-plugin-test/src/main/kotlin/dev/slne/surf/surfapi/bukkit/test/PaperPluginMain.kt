package dev.slne.surf.surfapi.bukkit.test

import dev.slne.surf.api.paper.plugin.SurfPaperBootstrapper
import dev.slne.surf.api.paper.plugin.SurfPaperPlugin

/**
 * The test plugin's main class; its logic lives in [TestPluginLifecycle] and the plugin's other
 * beans.
 */
class PaperPluginMain : SurfPaperPlugin() {

    companion object {
        val instance: PaperPluginMain
            get() = getPlugin(PaperPluginMain::class.java)
    }
}

/**
 * The test plugin's bootstrapper; its package is the root of the plugin's component scan.
 */
class PaperPluginBootstrapper : SurfPaperBootstrapper()

val plugin get() = PaperPluginMain.instance
