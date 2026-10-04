package dev.slne.surf.api.minestom.plugin

import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * The plugins the server loaded.
 */
interface PluginManager {

    /** Every loaded plugin, in the order they were loaded. */
    val plugins: Collection<SurfMinestomPlugin>

    /** The loaded plugin [id], or `null` when there is none. */
    fun plugin(id: String): SurfMinestomPlugin?

    /** Whether a plugin [id] is loaded. */
    fun isLoaded(id: String): Boolean = plugin(id) != null

    companion object {
        @Volatile
        private var installed: PluginManager? = null

        /**
         * The server's plugin manager.
         *
         * @throws IllegalStateException when the server does not load plugins
         */
        val instance: PluginManager
            get() = checkNotNull(installed) { "This server does not load plugins" }

        @InternalSurfApi
        fun install(manager: PluginManager?) {
            installed = manager
        }
    }
}
