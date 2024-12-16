package dev.slne.surf.surfapi.core.server.config

import dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.synchronize

object SpongeConfigTracker {
    private val configManagers =
        mutableObject2ObjectMapOf<Class<*>, SpongeConfigManager<*>>().synchronize()

    fun <C> getConfig(configClass: Class<C>): C? {
        return configManagers[configClass]?.config as? C
    }

    fun <C> reloadConfig(configClass: Class<C>): C {
        val manager =
            configManagers[configClass] ?: error("No config manager found for $configClass")

        return manager.reloadFromFile() as C
    }

    fun <C> registerConfig(configClass: Class<C>, configManager: SpongeConfigManager<C>) {
        configManagers[configClass] = configManager
    }

    @Suppress("UNCHECKED_CAST")
    fun <C> getConfigManager(configClass: Class<C>) = configManagers[configClass] as? SpongeConfigManager<C>
}