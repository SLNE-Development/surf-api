@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.config

import dev.slne.surf.api.config.manager.SpongeConfigManager
import dev.slne.surf.api.config.migration.ConfigMigrationBuilder
import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.nio.file.Path

/**
 * API for managing configuration files in the Surf API, supporting both Sponge and DazzlConf configurations.
 * Provides methods to create, retrieve, and reload configuration files in various formats (YAML, JSON).
 */
interface SurfConfigApi {
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
    ): SpongeConfigManager<C> =
        createSpongeYmlConfigManager(
            configClass,
            configFolder,
            configFileName,
            ConfigMigrationBuilder()
        )

    /**
     * Creates a Sponge YAML configuration manager.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @param configFolder The folder where the configuration file is stored.
     * @param configFileName The name of the configuration file. Must follow the YAML file name pattern.
     * @param migrations Optional migration builder with pre-registered migrations.
     * @return An instance of [SpongeConfigManager] for the configuration class [C].
     */
    fun <C> createSpongeYmlConfigManager(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @YamlConfigFileNamePattern String,
        migrations: ConfigMigrationBuilder,
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
    ): SpongeConfigManager<C> =
        createSpongeJsonConfigManager(
            configClass,
            configFolder,
            configFileName,
            ConfigMigrationBuilder()
        )

    /**
     * Creates a Sponge JSON configuration manager.
     *
     * @param C The type of the configuration class.
     * @param configClass The class of the configuration.
     * @param configFolder The folder where the configuration file is stored.
     * @param configFileName The name of the configuration file. Must follow the JSON file name pattern.
     * @param migrations Optional migration builder with pre-registered migrations.
     * @return An instance of [SpongeConfigManager] for the configuration class [C].
     */
    fun <C> createSpongeJsonConfigManager(
        configClass: Class<C>,
        configFolder: Path,
        configFileName: @JsonConfigFileNamePattern String,
        migrations: ConfigMigrationBuilder,
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

    /**
     * Reaches the one [SurfConfigApi] without delegating to it.
     *
     * This companion used to implement the interface by delegation, so that `SurfConfigApi.load`
     * meant the same as `surfConfigApi.load`. It cannot, now that the implementation is a bean:
     * delegation stores the delegate in a field, so the companion's initialiser has to resolve the
     * bean — and this interface's initialiser runs as part of initialising its own implementation,
     * which the container does while it is building that very bean. The result was a cycle the
     * container could never answer.
     *
     * Reading [instance] through a getter is what breaks it: the initialiser stores nothing and
     * the lookup happens when someone actually asks.
     */
    companion object {
        /** The one [SurfConfigApi], resolved on first use. */
        val instance get() = surfConfigApi
    }
}

/**
 * Retrieves the singleton instance of [SurfConfigApi].
 */
val surfConfigApi by surfBean<SurfConfigApi>()

/**
 * Creates a Sponge YAML configuration using a reified type.
 */
inline fun <reified C> SurfConfigApi.createSpongeYmlConfig(
    configFolder: Path,
    configFileName: @YamlConfigFileNamePattern String,
) = createSpongeYmlConfig(C::class.java, configFolder, configFileName)

/**
 * Creates a Sponge YAML configuration manager using a reified type.
 */
@JvmOverloads
inline fun <reified C> SurfConfigApi.createSpongeYmlConfigManager(
    configFolder: Path,
    configFileName: @YamlConfigFileNamePattern String,
    migrations: ConfigMigrationBuilder = ConfigMigrationBuilder(),
) = createSpongeYmlConfigManager(C::class.java, configFolder, configFileName, migrations)

/**
 * Creates a Sponge JSON configuration using a reified type.
 */
inline fun <reified C> SurfConfigApi.createSpongeJsonConfig(
    configFolder: Path,
    configFileName: @JsonConfigFileNamePattern String,
) = createSpongeJsonConfig(C::class.java, configFolder, configFileName)

/**
 * Creates a Sponge JSON configuration manager using a reified type.
 */
@JvmOverloads
inline fun <reified C> SurfConfigApi.createSpongeJsonConfigManager(
    configFolder: Path,
    configFileName: @JsonConfigFileNamePattern String,
    migrations: ConfigMigrationBuilder = ConfigMigrationBuilder(),
) = createSpongeJsonConfigManager(C::class.java, configFolder, configFileName, migrations)

/**
 * Retrieves a Sponge configuration using a reified type.
 */
inline fun <reified C> SurfConfigApi.getSpongeConfig() = getSpongeConfig(C::class.java)