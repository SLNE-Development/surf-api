package dev.slne.surf.api.paper.plugin

import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import kotlinx.coroutines.CoroutineScope
import org.bukkit.plugin.Plugin

/**
 * The part of surf-api's Paper runtime that drives plugin contexts.
 *
 * surf-api installs its implementation in its own bootstrap phase. [SurfPaperBootstrapper] and
 * [SurfPaperPlugin] forward their phases here; plugins never call it themselves.
 */
@InternalSurfApi
interface SurfPaperPluginRuntime {

    /**
     * Builds the context of the plugin that [bootstrapper] belongs to and runs its bootstrap hooks.
     */
    @Suppress("UnstableApiUsage")
    fun bootstrap(bootstrapper: SurfPaperBootstrapper, context: BootstrapContext)

    /**
     * Makes [plugin] injectable in its context and runs the load hooks.
     */
    fun load(plugin: SurfPaperPlugin)

    /**
     * Runs the enable hooks of [plugin]'s context.
     */
    fun enable(plugin: SurfPaperPlugin)

    /**
     * Runs the disable hooks of [plugin]'s context and closes it.
     */
    fun disable(plugin: SurfPaperPlugin)

    /**
     * The coroutine scope of [plugin]: its context's `CoroutineScope` bean, or surf-api's own
     * scope for surf-api's plugin.
     *
     * @throws IllegalStateException if [plugin] does not run on surf-api or has not loaded
     */
    fun coroutineScope(plugin: Plugin): CoroutineScope

    /**
     * The [PaperDispatchers] of [plugin], or surf-api's own for surf-api's plugin.
     *
     * @throws IllegalStateException if [plugin] does not run on surf-api or has not loaded
     */
    fun dispatchers(plugin: Plugin): PaperDispatchers

    @InternalSurfApi
    companion object {

        @Volatile
        private var installed: SurfPaperPluginRuntime? = null

        /**
         * The installed runtime.
         *
         * @throws IllegalStateException if surf-api has not bootstrapped, which means the calling
         * plugin does not declare `SurfPaperAPI` as a bootstrap dependency that loads before it
         */
        fun get(): SurfPaperPluginRuntime = checkNotNull(installed) {
            "surf-api has not bootstrapped yet. Declare SurfPaperAPI as a bootstrap dependency " +
                    "with load order BEFORE and join-classpath enabled."
        }

        /**
         * Installs [runtime]; called once by surf-api's bootstrapper.
         */
        fun install(runtime: SurfPaperPluginRuntime) {
            check(installed == null) { "A SurfPaperPluginRuntime is already installed" }
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
