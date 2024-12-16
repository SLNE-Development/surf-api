package dev.slne.surf.surfapi.core.server.config

import dev.slne.surf.surfapi.core.api.config.YamlConfigFileNamePattern
import dev.slne.surf.surfapi.core.api.config.manager.DazzlConfConfigManager
import dev.slne.surf.surfapi.core.api.config.manager.PreferUsingSpongeConfigOverDazzlConf
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.synchronize
import java.nio.file.Path

@PreferUsingSpongeConfigOverDazzlConf
object DazzlConfConfigTracker {
    private val configManagers =
        mutableObject2ObjectMapOf<Class<*>, DazzlConfConfigManager<*>>().synchronize()

    @Suppress("UNCHECKED_CAST")
    fun <C> getConfig(configClass: Class<C>): C {
        val manager =
            configManagers[configClass] ?: error("No config manager found for $configClass")

        return manager.config as C
    }

    @Suppress("UNCHECKED_CAST")
    fun <C> reloadConfig(configClass: Class<C>): C {
        val manager =
            configManagers[configClass] ?: error("No config manager found for $configClass")

        return manager.reloadConfig() as C
    }

    fun <C> registerConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String
    ) {
        configManagers[configClass] =
            DazzlConfConfigManager.create(configClass, configFolder, configFileName)
    }
}