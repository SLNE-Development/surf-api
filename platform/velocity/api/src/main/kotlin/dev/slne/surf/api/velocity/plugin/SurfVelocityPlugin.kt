package dev.slne.surf.api.velocity.plugin

import com.velocitypowered.api.plugin.PluginContainer

/**
 * The main class of a Velocity plugin that runs on surf-api.
 *
 * Velocity creates the main class through Guice, so a subclass declares an `@Inject` constructor
 * that forwards the plugin's [PluginContainer]:
 *
 * ```kotlin
 * @Plugin(id = "shop", name = "Shop", dependencies = [Dependency(id = "surf-api-velocity")])
 * class ShopPlugin @Inject constructor(container: PluginContainer) : SurfVelocityPlugin(container)
 * ```
 *
 * The constructor is the plugin's load phase: it builds the plugin's Spring context and runs the
 * [dev.slne.surf.api.spring.SurfLifecycle.onLoad] hooks of its beans. surf-api enables the plugin in
 * its own `ProxyInitializeEvent` handler and disables it in its `ProxyShutdownEvent` handler, so the
 * plugin must declare `surf-api-velocity` as a dependency to be constructed after surf-api. Put
 * plugin logic into beans instead of this class.
 *
 * The instance is a bean of its own context, primary under its concrete type. Velocity registers
 * the instance as an event listener itself; surf-api registers the other beans with `@Subscribe`
 * methods. It registers them while Velocity fires `ProxyInitializeEvent`, so their handlers for that
 * event never run: a bean that has to act once the proxy is up overrides
 * [dev.slne.surf.api.spring.SurfLifecycle.onEnable].
 *
 * A plugin named in [dev.slne.surf.api.spring.ConditionalOnPlugin] has to be a dependency of this
 * plugin, optional for a soft one: the condition is evaluated in the constructor, and Velocity knows
 * only the plugins constructed before.
 *
 * @param container the plugin's own container, which Velocity injects
 * @throws IllegalStateException if surf-api has not loaded, or if the plugin's context cannot be
 * built
 */
abstract class SurfVelocityPlugin(container: PluginContainer) {

    init {
        @Suppress("LeakingThis")
        SurfVelocityPluginRuntime.get().load(this, container)
    }
}
