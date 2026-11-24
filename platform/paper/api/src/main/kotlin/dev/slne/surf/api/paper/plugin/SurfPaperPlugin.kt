package dev.slne.surf.api.paper.plugin

import org.bukkit.plugin.java.JavaPlugin

/**
 * The main class of a Paper plugin that runs on surf-api.
 *
 * Its lifecycle methods are final: surf-api drives the plugin's Spring context through them and
 * calls the [dev.slne.surf.api.spring.SurfLifecycle] hooks of the plugin's beans. Put plugin logic
 * into beans instead of this class.
 *
 * The plugin needs a [SurfPaperBootstrapper] as well, because its context is built in Paper's
 * bootstrap phase.
 */
abstract class SurfPaperPlugin : JavaPlugin() {

    final override fun onLoad(): Unit = SurfPaperPluginRuntime.get().load(this)

    final override fun onEnable(): Unit = SurfPaperPluginRuntime.get().enable(this)

    final override fun onDisable(): Unit = SurfPaperPluginRuntime.get().disable(this)
}
