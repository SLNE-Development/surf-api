package dev.slne.surf.api.velocity.plugin

import com.google.inject.Inject
import com.velocitypowered.api.plugin.PluginContainer

/**
 * The main class of a Velocity plugin that runs on surf-api.
 *
 * Velocity creates the main class through Guice. A subclass takes whatever its constructor needs
 * from Guice, or declares no constructor at all:
 *
 * ```kotlin
 * @Plugin(id = "shop", name = "Shop", dependencies = [Dependency(id = "surf-api-velocity")])
 * class ShopPlugin @Inject constructor(val proxy: ProxyServer) : SurfVelocityPlugin()
 * ```
 *
 * The plugin's load phase runs when Guice injects this class's members, after the subclass
 * constructor has finished and before Velocity creates the next plugin: surf-api builds the
 * plugin's Spring context and runs the [dev.slne.surf.api.spring.SurfLifecycle.onLoad] hooks of
 * its beans. Guice injects superclass members first, so `@Inject` fields and methods of the
 * subclass are not yet injected while the load phase runs. surf-api enables the plugin in its own
 * `ProxyInitializeEvent` handler and disables it in its `ProxyShutdownEvent` handler, so the plugin
 * must declare `surf-api-velocity` as a dependency to be created after surf-api. Put plugin logic
 * into beans instead of this class.
 *
 * The instance is a bean of its own context, primary under its concrete type. Velocity registers
 * the instance as an event listener itself; surf-api registers the other beans with `@Subscribe`
 * methods. It registers them while Velocity fires `ProxyInitializeEvent`, so their handlers for
 * that event never run: a bean that has to act once the proxy is up overrides
 * [dev.slne.surf.api.spring.SurfLifecycle.onEnable].
 *
 * A plugin named in [dev.slne.surf.api.spring.ConditionalOnPlugin] has to be a dependency of this
 * plugin, optional for a soft one: the condition is evaluated in the load phase, and Velocity knows
 * only the plugins created before.
 */
abstract class SurfVelocityPlugin {

    /**
     * Runs the plugin's load phase; Guice calls it once the instance is constructed.
     *
     * @param container the plugin's own container
     * @throws IllegalStateException if surf-api has not loaded, or if the plugin's context cannot
     * be built
     */
    @Inject
    private fun loadSurfContext(container: PluginContainer) {
        SurfVelocityPluginRuntime.get().load(this, container)
    }
}
