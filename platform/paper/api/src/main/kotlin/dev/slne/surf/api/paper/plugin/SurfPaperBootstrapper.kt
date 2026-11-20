package dev.slne.surf.api.paper.plugin

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap

/**
 * The bootstrapper of a Paper plugin that runs on surf-api.
 *
 * surf-api builds the plugin's Spring context here, in Paper's bootstrap phase, and runs the
 * [dev.slne.surf.api.spring.SurfBootstrapLifecycle] hooks. The subclass must live in the plugin's own jar: its class loader and code
 * source tell surf-api which classes belong to the plugin. Components are scanned from the
 * subclass's package downwards.
 *
 * The plugin must declare `SurfPaperAPI` and every surf plugin it depends on as bootstrap
 * dependencies that load before it, so that their contexts exist when this one is built.
 */
@Suppress("UnstableApiUsage")
abstract class SurfPaperBootstrapper : PluginBootstrap {

    final override fun bootstrap(context: BootstrapContext) {
        SurfPaperPluginRuntime.get().bootstrap(this, context)
    }
}
