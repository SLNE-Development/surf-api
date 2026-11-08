package dev.slne.surf.api.core.config

import java.nio.file.Path

/**
 * Convenience base class for YAML-backed Sponge configuration classes.
 *
 * This class wires the common configuration metadata ([configFolder], [fileName])
 * to a YAML-based [SpongeConfigManager][dev.slne.surf.api.core.api.config.manager.SpongeConfigManager] instance created by [surfConfigApi].
 *
 * Typical usage is via a companion object on a `@ConfigSerializable` data class:
 * ```kotlin
 * @ConfigSerializable
 * data class MyConfig(
 *     var someField: String = "value"
 * ) {
 *     companion object : SpongeYmlConfigClass<MyConfig>(
 *         MyConfig::class.java,
 *         Path("config/my-plugin"),
 *         "my-config.yml"
 *     ) {
 *         init {
 *             migration(1, MyFirstMigration)
 *         }
 *     }
 * }
 * ```
 *
 * @param C the type of the configuration data object.
 * @param configClass the Java class of [C], used by the underlying config framework.
 */
abstract class SpongeYmlConfigClass<C>(
    configClass: Class<C>, configFolder: Path, fileName: String
) : SpongeConfigClass<C>(configClass, configFolder, fileName) {

    private val managerLazy = lazy {
        surfConfigApi.createSpongeYmlConfigManager(
            configClass,
            configFolder,
            fileName,
            migrationBuilder
        )
    }

    /**
     * YAML-backed configuration manager for this config type.
     *
     * The manager is created using [SurfConfigApi.createSpongeYmlConfigManager]
     * with [configClass], [configFolder], [fileName] and [migrationBuilder].
     */
    final override val manager by managerLazy

    final override fun isInitialized(): Boolean {
        return managerLazy.isInitialized()
    }
}