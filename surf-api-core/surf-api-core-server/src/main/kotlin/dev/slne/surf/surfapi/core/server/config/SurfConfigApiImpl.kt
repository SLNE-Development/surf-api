package dev.slne.surf.surfapi.core.server.config

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.config.JsonConfigFileNamePattern
import dev.slne.surf.surfapi.core.api.config.SurfConfigApi
import dev.slne.surf.surfapi.core.api.config.YamlConfigFileNamePattern
import dev.slne.surf.surfapi.core.api.config.manager.PreferUsingSpongeConfigOverDazzlConf
import dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import java.nio.file.Path

@AutoService(SurfConfigApi::class)
class SurfConfigApiImpl : SurfConfigApi {
    init {
        checkInstantiationByServiceLoader()
    }

    @PreferUsingSpongeConfigOverDazzlConf
    override fun <C> createDazzlConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String
    ): C {
        DazzlConfConfigTracker.registerConfig(configClass, configFolder, configFileName)
        DazzlConfConfigTracker.reloadConfig(configClass)
        return DazzlConfConfigTracker.getConfig(configClass)
    }

    @PreferUsingSpongeConfigOverDazzlConf
    override fun <C> getDazzlConfig(configClass: Class<C>): C =
        DazzlConfConfigTracker.getConfig(configClass)

    @PreferUsingSpongeConfigOverDazzlConf
    override fun <C> reloadDazzlConfig(configClass: Class<C>): C =
        DazzlConfConfigTracker.reloadConfig(configClass)

    override fun <C> createSpongeYmlConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String
    ): C {
        val manager = SpongeConfigManager.yaml(configClass, configFolder, configFileName)
        SpongeConfigTracker.registerConfig(configClass, manager)

        return manager.config
    }

    override fun <C> createSpongeJsonConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @JsonConfigFileNamePattern String
    ): C {
        val manager = SpongeConfigManager.json(configClass, configFolder, configFileName)
        SpongeConfigTracker.registerConfig(configClass, manager)

        return manager.config
    }

    override fun <C> getSpongeConfig(configClass: Class<C>): C =
        SpongeConfigTracker.getConfig(configClass) ?: error("No config found for $configClass")

    override fun <C> reloadSpongeConfig(configClass: Class<C>): C =
        SpongeConfigTracker.reloadConfig(configClass)

    override fun <C> getSpongeConfigManagerForConfig(configClass: Class<C>): SpongeConfigManager<C> =
        SpongeConfigTracker.getConfigManager(configClass)
            ?: error("No config manager found for $configClass")
}