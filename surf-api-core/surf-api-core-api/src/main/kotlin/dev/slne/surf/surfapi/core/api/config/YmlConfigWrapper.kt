package dev.slne.surf.surfapi.core.api.config

import dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * Wraps a configuration file in YAML format to provide convenient access and modifications via a configuration class.
 * Automatically manages the creation, loading, and saving of the configuration through a `SpongeConfigManager`.
 *
 * @param T The type of the configuration class.
 * @param clazz The class of the configuration.
 * @param configFolder The folder where the configuration file is located.
 * @param fileName The name of the configuration file in YAML format.
 *
 * Use in your main: val myConfig = surfConfigApi.createYmlConfig<MyConfigClass>(configFolderPath)
 */
class YmlConfigWrapper<T : Any>(
    private val clazz: KClass<T>,
    private val configFolder: Path,
    private val fileName: String
) {
    /**
     * Manages the configuration of type [T] through a Sponge-based configuration manager.
     * Handles loading, saving, and reloading of the configuration using the specified file and folder structure.
     * The configuration manager ensures that operations such as edits or reloads are applied to the configuration instance.
     *
     * @see SpongeConfigManager
     */
    private val configManager: SpongeConfigManager<T>

    init {
        surfConfigApi.createSpongeYmlConfig(clazz.java, configFolder, fileName)
        configManager = surfConfigApi.getSpongeConfigManagerForConfig(clazz.java)
        reload()
    }

    /**
     * Reloads the configuration by delegating the operation to the underlying configuration manager.
     *
     * This method ensures that the configuration is reloaded from the associated file,
     * updating the current in-memory representation of the configuration. If a critical error
     * occurs during the reload process, it propagates the exception thrown by the configuration manager.
     *
     * Recommended to be used when changes to the underlying configuration file have been made and
     * need to be reflected within the application.
     *
     * @throws RuntimeException if a critical error occurs during the reload operation.
     */
    fun reload() {
        configManager.reloadFromFile()
    }

    /**
     * Edits the configuration by applying the given block of modifications.
     * Optionally saves the changes to the configuration file.
     *
     * @param save Indicates whether the updated configuration should be saved to the file.
     *             Defaults to `true`, meaning the changes will be persisted.
     * @param block A lambda function defining the modifications to be applied to the configuration object.
     */
    fun edit(save: Boolean = true, block: T.() -> Unit) {
        val config = config
        config.block()

        if (save) {
            configManager.save()
        }
    }

    /**
     * Provides access to the configuration object managed by the `SpongeConfigManager`.
     * This property retrieves the current state of the configuration.
     *
     * @return The configuration data of type [T].
     */
    val config: T get() = configManager.config
}

/**
 * Creates a YAML configuration wrapper for a specified configuration class.
 *
 * @param C The type of the configuration class.
 * @param configFolder The folder where the configuration file will be stored.
 * @param fileName The name of the configuration file, defaulting to "config.yml".
 * @return A `YmlConfigWrapper` instance for managing the YAML configuration of type [C].
 */
inline fun <reified C : Any> SurfConfigApi.createYmlConfig(
    configFolder: Path,
    fileName: String = "config.yml"
): YmlConfigWrapper<C> {
    return YmlConfigWrapper(C::class, configFolder, fileName)
}