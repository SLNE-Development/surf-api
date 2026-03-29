package dev.slne.surf.surfapi.core.api.config

import java.nio.file.Path

/**
 * Convenience base class for JSON-backed Sponge configuration classes.
 *
 * This class wires the common configuration metadata ([configFolder], [fileName])
 * to a JSON-based [SpongeConfigManager][dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager] instance created by [surfConfigApi].
 *
 * Typical usage is via a companion object on a `@ConfigSerializable` data class:
 * ```kotlin
 * @ConfigSerializable
 * data class MyJsonConfig(
 *     var someField: String = "value"
 * ) {
 *     companion object : SpongeJsonConfigClass<MyJsonConfig>(
 *         MyJsonConfig::class.java,
 *         Path("config/my-plugin"),
 *         "my-config.json"
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
abstract class SpongeJsonConfigClass<C>(
    configClass: Class<C>, configFolder: Path, fileName: String
) : SpongeConfigClass<C>(configClass, configFolder, fileName) {

    /**
     * JSON-backed configuration manager for this config type.
     *
     * The manager is created using [SurfConfigApi.createSpongeJsonConfigManager]
     * with [configClass], [configFolder], [fileName] and [migrationBuilder].
     */
    override val manager by lazy {
        surfConfigApi.createSpongeJsonConfigManager(configClass, configFolder, fileName, migrationBuilder)
    }
}