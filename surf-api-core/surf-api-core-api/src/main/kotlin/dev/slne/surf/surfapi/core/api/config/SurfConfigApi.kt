package dev.slne.surf.surfapi.core.api.config

import dev.slne.surf.surfapi.core.api.config.manager.PreferUsingSpongeConfigOverDazzlConf
import dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager
import dev.slne.surf.surfapi.core.api.util.requiredService
import java.nio.file.Path

interface SurfConfigApi {

    @PreferUsingSpongeConfigOverDazzlConf
    fun <C> createDazzlConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String
    ): C

    @PreferUsingSpongeConfigOverDazzlConf
    fun <C> getDazzlConfig(configClass: Class<C>): C

    @PreferUsingSpongeConfigOverDazzlConf
    fun <C> reloadDazzlConfig(configClass: Class<C>): C

    fun <C> createSpongeYmlConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String
    ): C

    fun <C> createSpongeJsonConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @JsonConfigFileNamePattern String
    ): C

    fun <C> getSpongeConfig(configClass: Class<C>): C

    fun <C> reloadSpongeConfig(configClass: Class<C>): C

    fun <C> getSpongeConfigManagerForConfig(configClass: Class<C>): SpongeConfigManager<C>

    companion object {
        val instance = requiredService<SurfConfigApi>()
    }
}

val surfConfigApi get() = SurfConfigApi.instance

@PreferUsingSpongeConfigOverDazzlConf
inline fun <reified C> SurfConfigApi.createDazzlConfig(
    configFolder: Path,
    configFileName: @YamlConfigFileNamePattern String
) = createDazzlConfig(C::class.java, configFolder, configFileName)

@PreferUsingSpongeConfigOverDazzlConf
inline fun <reified C> SurfConfigApi.getDazzlConfig() = getDazzlConfig(C::class.java)

@PreferUsingSpongeConfigOverDazzlConf
inline fun <reified C> SurfConfigApi.reloadDazzlConfig() = reloadDazzlConfig(C::class.java)

inline fun <reified C> SurfConfigApi.createSpongeYmlConfig(
    configFolder: Path,
    configFileName: @YamlConfigFileNamePattern String
) = createSpongeYmlConfig(C::class.java, configFolder, configFileName)

inline fun <reified C> SurfConfigApi.createSpongeJsonConfig(
    configFolder: Path,
    configFileName: @JsonConfigFileNamePattern String
) = createSpongeJsonConfig(C::class.java, configFolder, configFileName)

inline fun <reified C> SurfConfigApi.getSpongeConfig() = getSpongeConfig(C::class.java)