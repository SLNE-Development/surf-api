package dev.slne.surf.api.core.server.config

import dev.slne.surf.api.core.config.JsonConfigFileNamePattern
import dev.slne.surf.api.core.config.SurfConfigApi
import dev.slne.surf.api.core.config.YamlConfigFileNamePattern
import dev.slne.surf.api.core.config.manager.SpongeConfigManager
import dev.slne.surf.api.core.config.migration.ConfigMigrationBuilder
import dev.slne.surf.api.di.Service
import java.nio.file.Path

@Service
class SurfConfigApiImpl : SurfConfigApi {
    override fun <C> createSpongeYmlConfigManager(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String,
        migrations: ConfigMigrationBuilder
    ): SpongeConfigManager<C> {
        val manager =
            SpongeConfigManager.yaml(configClass, configFolder, configFileName, migrations)
        SpongeConfigTracker.registerConfig(configClass, manager)

        return manager
    }

    override fun <C> createSpongeJsonConfigManager(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @JsonConfigFileNamePattern String,
        migrations: ConfigMigrationBuilder
    ): SpongeConfigManager<C> {
        val manager =
            SpongeConfigManager.json(configClass, configFolder, configFileName, migrations)
        SpongeConfigTracker.registerConfig(configClass, manager)

        return manager
    }

    override fun <C> createSpongeYmlConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String
    ): C {
        return createSpongeYmlConfigManager(configClass, configFolder, configFileName).config
    }

    override fun <C> createSpongeJsonConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @JsonConfigFileNamePattern String
    ): C {
        return createSpongeJsonConfigManager(configClass, configFolder, configFileName).config
    }

    override fun <C> getSpongeConfig(configClass: Class<C>): C =
        SpongeConfigTracker.getConfig(configClass) ?: error("No config found for $configClass")

    override fun <C> reloadSpongeConfig(configClass: Class<C>): C =
        SpongeConfigTracker.reloadConfig(configClass)

    override fun <C> getSpongeConfigManagerForConfig(configClass: Class<C>): SpongeConfigManager<C> =
        SpongeConfigTracker.getConfigManager(configClass)
            ?: error("No config manager found for $configClass")
}