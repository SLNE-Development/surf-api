package dev.slne.surf.surfapi.core.api.config

import dev.slne.surf.surfapi.core.api.config.manager.DazzlConfDeprecationMessageHolder
import dev.slne.surf.surfapi.core.api.config.manager.PreferUsingSpongeConfigOverDazzlConf
import dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager
import dev.slne.surf.surfapi.core.api.util.requiredService
import java.nio.file.Path

/**
 * API for managing configuration files in the Surf API, supporting both Sponge and DazzlConf configurations.
 * Provides methods to create, retrieve, and reload configuration files in various formats (YAML, JSON).
 */
interface SurfConfigApi {

    /**
     * Creates a DazzlConf configuration file.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @param configFolder The folder where the configuration file is stored.
     * @param configFileName The name of the configuration file. Must follow the YAML file name pattern.
     * @return An instance of the configuration class [C].
     */
    @PreferUsingSpongeConfigOverDazzlConf
    @Deprecated(message = DazzlConfDeprecationMessageHolder.MESSAGE, level = DeprecationLevel.ERROR)
    fun <C> createDazzlConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String,
    ): C

    /**
     * Retrieves a DazzlConf configuration.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @return An instance of the configuration class [C].
     */
    @PreferUsingSpongeConfigOverDazzlConf
    @Deprecated(message = DazzlConfDeprecationMessageHolder.MESSAGE, level = DeprecationLevel.ERROR)
    fun <C> getDazzlConfig(configClass: Class<C>): C

    /**
     * Reloads a DazzlConf configuration from the file.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @return The reloaded instance of the configuration class [C].
     */
    @PreferUsingSpongeConfigOverDazzlConf
    @Deprecated(message = DazzlConfDeprecationMessageHolder.MESSAGE, level = DeprecationLevel.ERROR)
    fun <C> reloadDazzlConfig(configClass: Class<C>): C

    /**
     * Creates a Sponge YAML configuration file.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @param configFolder The folder where the configuration file is stored.
     * @param configFileName The name of the configuration file. Must follow the YAML file name pattern.
     * @return An instance of the configuration class [C].
     */
    fun <C> createSpongeYmlConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String,
    ): C

    /**
     * Creates a Sponge YAML configuration manager.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @param configFolder The folder where the configuration file is stored.
     * @param configFileName The name of the configuration file. Must follow the YAML file name pattern.
     * @return An instance of [SpongeConfigManager] for the configuration class [C].
     */
    fun <C> createSpongeYmlConfigManager(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String,
    ): SpongeConfigManager<C>

    /**
     * Creates a Sponge JSON configuration file.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @param configFolder The folder where the configuration file is stored.
     * @param configFileName The name of the configuration file. Must follow the JSON file name pattern.
     * @return An instance of the configuration class [C].
     */
    fun <C> createSpongeJsonConfig(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @JsonConfigFileNamePattern String,
    ): C

    /**
     * Creates a Sponge JSON configuration manager.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @param configFolder The folder where the configuration file is stored.
     * @param configFileName The name of the configuration file. Must follow the JSON file name pattern.
     * @return An instance of [SpongeConfigManager] for the configuration class [C].
     */
    fun <C> createSpongeJsonConfigManager(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @JsonConfigFileNamePattern String,
    ): SpongeConfigManager<C>

    /**
     * Retrieves a Sponge configuration.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @return An instance of the configuration class [C].
     */
    fun <C> getSpongeConfig(configClass: Class<C>): C

    /**
     * Reloads a Sponge configuration from the file.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @return The reloaded instance of the configuration class [C].
     */
    fun <C> reloadSpongeConfig(configClass: Class<C>): C

    /**
     * Retrieves the [SpongeConfigManager] for a specific configuration class.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @return An instance of [SpongeConfigManager] for the configuration class [C].
     */
    fun <C> getSpongeConfigManagerForConfig(configClass: Class<C>): SpongeConfigManager<C>

    companion object: SurfConfigApi by surfConfigApi {
        /**
         * Retrieves the singleton instance of [SurfConfigApi].
         */
        val instance = surfConfigApi
    }
}

/**
 * Retrieves the singleton instance of [SurfConfigApi].
 */
val surfConfigApi = requiredService<SurfConfigApi>()

/**
 * Creates a DazzlConf configuration using a reified type.
 *
 * @param C The type of the configuration class.
 * @param configFolder The folder where the configuration file is stored.
 * @param configFileName The name of the configuration file. Must follow the YAML file name pattern.
 * @return An instance of the configuration class [C].
 */
@PreferUsingSpongeConfigOverDazzlConf
@Deprecated(message = DazzlConfDeprecationMessageHolder.MESSAGE, level = DeprecationLevel.ERROR)
inline fun <reified C> SurfConfigApi.createDazzlConfig(
    configFolder: Path,
    configFileName: @YamlConfigFileNamePattern String,
) = createDazzlConfig(C::class.java, configFolder, configFileName)

/**
 * Retrieves a DazzlConf configuration using a reified type.
 *
 * @param C The type of the configuration class.
 * @return An instance of the configuration class [C].
 */
@PreferUsingSpongeConfigOverDazzlConf
@Deprecated(message = DazzlConfDeprecationMessageHolder.MESSAGE, level = DeprecationLevel.ERROR)
inline fun <reified C> SurfConfigApi.getDazzlConfig() = getDazzlConfig(C::class.java)

/**
 * Reloads a DazzlConf configuration using a reified type.
 *
 * @param C The type of the configuration class.
 * @return The reloaded instance of the configuration class [C].
 */
@PreferUsingSpongeConfigOverDazzlConf
@Deprecated(message = DazzlConfDeprecationMessageHolder.MESSAGE, level = DeprecationLevel.ERROR)
inline fun <reified C> SurfConfigApi.reloadDazzlConfig() = reloadDazzlConfig(C::class.java)

/**
 * Creates a Sponge YAML configuration using a reified type.
 *
 * @param C The type of the configuration class.
 * @param configFolder The folder where the configuration file is stored.
 * @param configFileName The name of the configuration file. Must follow the YAML file name pattern.
 * @return An instance of the configuration class [C].
 */
inline fun <reified C> SurfConfigApi.createSpongeYmlConfig(
    configFolder: Path,
    configFileName: @YamlConfigFileNamePattern String,
) = createSpongeYmlConfig(C::class.java, configFolder, configFileName)

/**
 * Creates a Sponge YAML configuration manager using a reified type.
 *
 * @param C The type of the configuration class.
 * @param configFolder The folder where the configuration file is stored.
 * @param configFileName The name of the configuration file. Must follow the YAML file name pattern.
 * @return An instance of [SpongeConfigManager] for the configuration class [C].
 */
inline fun <reified C> SurfConfigApi.createSpongeYmlConfigManager(
    configFolder: Path,
    configFileName: @YamlConfigFileNamePattern String,
) = createSpongeYmlConfigManager(C::class.java, configFolder, configFileName)

/**
 * Creates a Sponge JSON configuration using a reified type.
 *
 * @param C The type of the configuration class.
 * @param configFolder The folder where the configuration file is stored.
 * @param configFileName The name of the configuration file. Must follow the JSON file name pattern.
 * @return An instance of the configuration class [C].
 */
inline fun <reified C> SurfConfigApi.createSpongeJsonConfig(
    configFolder: Path,
    configFileName: @JsonConfigFileNamePattern String,
) = createSpongeJsonConfig(C::class.java, configFolder, configFileName)

/**
 * Creates a Sponge JSON configuration manager using a reified type.
 *
 * @param C The type of the configuration class.
 * @param configFolder The folder where the configuration file is stored.
 * @param configFileName The name of the configuration file. Must follow the JSON file name pattern.
 * @return An instance of [SpongeConfigManager] for the configuration class [C].
 */
inline fun <reified C> SurfConfigApi.createSpongeJsonConfigManager(
    configFolder: Path,
    configFileName: @JsonConfigFileNamePattern String,
) = createSpongeJsonConfigManager(C::class.java, configFolder, configFileName)

/**
 * Retrieves a Sponge configuration using a reified type.
 *
 * @param C The type of the configuration class.
 * @return An instance of the configuration class [C].
 */
inline fun <reified C> SurfConfigApi.getSpongeConfig() = getSpongeConfig(C::class.java)