package dev.slne.surf.api.velocity.plugin

import com.velocitypowered.api.plugin.PluginContainer
import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * The part of surf-api's Velocity runtime that builds plugin contexts.
 *
 * surf-api installs its implementation while its own plugin is constructed. [SurfVelocityPlugin]
 * forwards its load phase here; plugins never call it themselves. Unlike on Paper, enabling and
 * disabling need no hook in the plugin class: surf-api drives them from its own proxy events.
 */
@InternalSurfApi
interface SurfVelocityPluginRuntime {

    /**
     * Builds the context of [plugin], makes it injectable in that context and runs the load hooks.
     *
     * Called from the plugin's constructor, so [container] has no instance yet. Nothing of the
     * context stays open if this throws.
     */
    fun load(plugin: SurfVelocityPlugin, container: PluginContainer)

    @InternalSurfApi
    companion object {

        @Volatile
        private var installed: SurfVelocityPluginRuntime? = null

        /**
         * The installed runtime.
         *
         * @throws IllegalStateException if surf-api has not loaded, which means the calling plugin
         * does not declare `surf-api-velocity` as a dependency
         */
        fun get(): SurfVelocityPluginRuntime = checkNotNull(installed) {
            "surf-api has not loaded yet. Declare the plugin 'surf-api-velocity' as a dependency " +
                    "of this plugin, so that Velocity constructs surf-api first."
        }

        /**
         * Installs [runtime]; called once by surf-api's own plugin.
         */
        fun install(runtime: SurfVelocityPluginRuntime) {
            check(installed == null) { "A SurfVelocityPluginRuntime is already installed" }
            installed = runtime
        }

        /**
         * Removes the installed runtime when surf-api shuts down.
         */
        fun uninstall() {
            installed = null
        }
    }
}
