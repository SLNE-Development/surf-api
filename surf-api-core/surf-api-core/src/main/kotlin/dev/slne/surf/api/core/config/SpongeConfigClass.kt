package dev.slne.surf.api.core.config

import dev.slne.surf.api.core.config.manager.SpongeConfigManager
import dev.slne.surf.api.core.config.migration.ConfigMigration
import dev.slne.surf.api.core.config.migration.ConfigMigrationBuilder
import org.spongepowered.configurate.ConfigurationNode
import java.nio.file.Path

/**
 * Base convenience wrapper around a [SpongeConfigManager] for a specific config type [C].
 *
 * This class is intended to be extended by companion objects of config data classes.
 * It provides a simple, type-safe API to:
 * - access the current config instance via [getConfig]
 * - persist changes via [save]
 * - reload the config from disk via [reloadFromFile]
 * - perform in-place mutations via [edit]
 * - register schema migrations via [migration]
 *
 * The actual manager instance is provided by subclasses and typically created by
 * a central configuration API (e.g. [surfConfigApi]).
 *
 * ## Versioned Migrations
 *
 * Migrations can be registered in the `init` block of companion objects:
 *
 * ```kotlin
 * @ConfigSerializable
 * data class MyConfig(
 *     var release: String = "1.0.0"
 * ) {
 *     companion object : SpongeYmlConfigClass<MyConfig>(
 *         MyConfig::class.java,
 *         Path("config/my-plugin"),
 *         "my-config.yml"
 *     ) {
 *         init {
 *             migration(1, RenameServerVersionMigration)
 *             migration(2) { node ->
 *                 // inline migration
 *                 node.node("old-field").raw(null)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param C the type of the configuration data object.
 * @param configClass the Java class of [C], used by underlying config frameworks
 * to create and map configuration instances.
 */
sealed class SpongeConfigClass<C>(
    configClass: Class<C>,
    /**
     * Folder where the configuration file is stored.
     *
     * Implementations should point this to a plugin- or module-specific config directory.
     */
    protected val configFolder: Path,
    /**
     * The name of the configuration file, including its extension
     * (for example `settings.yml` or `settings.json`).
     */
    protected val fileName: String
) {

    /**
     * The migration builder that collects all registered migrations.
     *
     * Subclasses should use [migration] to register migrations rather than
     * accessing this directly.
     */
    protected val migrationBuilder = ConfigMigrationBuilder()

    /**
     * The underlying configuration manager responsible for loading, saving,
     * and tracking the config instance of type [C].
     */
    abstract val manager: SpongeConfigManager<C>

    /**
     * Whether the config has been initialized and loaded from disk.
     */
    abstract fun isInitialized(): Boolean

    /**
     * Registers a migration for the given target [version].
     *
     * Migrations are applied in version order. For existing configs without a
     * version field, **all** registered migrations will be applied the first time.
     *
     * @param version the target version this migration upgrades to (must be >= 0)
     * @param migration the migration to apply
     */
    protected fun migration(version: Int, migration: ConfigMigration) {
        migrationBuilder.migration(version, migration)
    }

    /**
     * Registers an inline migration for the given target [version].
     *
     * ```kotlin
     * init {
     *     migration(1) { node ->
     *         node.node("old-key").raw(null)
     *     }
     * }
     * ```
     *
     * @param version the target version this migration upgrades to (must be >= 0)
     * @param migration the migration lambda
     */
    protected inline fun migration(
        version: Int,
        crossinline migration: (ConfigurationNode) -> Unit
    ) {
        migrationBuilder.migration(version, ConfigMigration { node -> migration(node) })
    }

    /**
     * Sets the path in the config file where the version number is stored.
     *
     * Defaults to `"_config_version"`. Call this in the `init` block before
     * any migration registrations if you need a custom key.
     *
     * @param path the path components to the version key
     */
    protected fun versionKey(vararg path: Any) {
        migrationBuilder.versionKey(*path)
    }

    /**
     * Persists the current configuration to disk.
     *
     * Delegates to [SpongeConfigManager.save].
     */
    fun save() = manager.save()

    /**
     * Reloads the configuration from disk and replaces the current in-memory instance.
     *
     * Delegates to [SpongeConfigManager.reloadFromFile].
     * Use this when external changes to the config file should be picked up at runtime.
     */
    fun reloadFromFile() = manager.reloadFromFile()

    /**
     * Applies mutations to the current config instance in a safe way.
     *
     * The [block] receives the current config instance as receiver and can freely
     * modify its properties. After the block completes, the config will be saved
     * automatically if [save] is `true`.
     *
     * Example:
     * ```kotlin
     * MyConfig.edit {
     *     someField = "new value"
     * }
     * ```
     *
     * @param save whether to persist the config to disk after applying [block]. Defaults to `true`.
     * @param block mutation block executed on the current config instance.
     */
    inline fun edit(save: Boolean = true, block: C.() -> Unit) = manager.edit(save, block)

    /**
     * Returns the current in-memory configuration instance.
     *
     * The returned object is managed by the underlying [SpongeConfigManager] and
     * will be replaced when [reloadFromFile] is called.
     */
    fun getConfig(): C = manager.config

    /**
     * Dummy initializer to force class loading and companion object initialization.
     *
     * This is useful in plugin entry points (e.g. `onLoad`) to ensure that:
     * - the config manager is constructed
     * - the configuration is loaded before first access via [getConfig]
     *
     * This method is a no-op and can be safely called multiple times.
     */
    fun init() {
        manager
    }
}