package dev.slne.surf.surfapi.core.api.config

import java.nio.file.Path

/**
 * Convenience base class for YAML-backed Sponge configuration classes.
 *
 * This class wires the common configuration metadata ([configFolder], [fileName])
 * to a YAML-based [dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager] instance created by [surfConfigApi].
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

    /**
     * YAML-backed configuration manager for this config type.
     *
     * The manager is created using [SurfConfigApi.createSpongeYmlConfigManager]
     * with [configClass], [configFolder] and [fileName].
     */
    override val manager =
        surfConfigApi.createSpongeYmlConfigManager(configClass, configFolder, fileName)
}