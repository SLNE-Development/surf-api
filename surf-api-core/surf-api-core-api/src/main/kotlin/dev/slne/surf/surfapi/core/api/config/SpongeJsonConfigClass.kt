package dev.slne.surf.surfapi.core.api.config

/**
 * Convenience base class for JSON-backed Sponge configuration classes.
 *
 * This class wires the common configuration metadata ([configFolder], [fileName])
 * to a JSON-based [dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager] instance created by [surfConfigApi].
 *
 * Typical usage is via a companion object on a `@ConfigSerializable` data class:
 * ```kotlin
 * @ConfigSerializable
 * data class MyJsonConfig(
 *     var someField: String = "value"
 * ) {
 *     companion object : SpongeJsonConfigClass<MyJsonConfig>(MyJsonConfig::class.java) {
 *         override val configFolder = Path("config/my-plugin")
 *         override val fileName = "my-config.json"
 *     }
 * }
 * ```
 *
 * @param C the type of the configuration data object.
 * @param configClass the Java class of [C], used by the underlying config framework.
 */
abstract class SpongeJsonConfigClass<C>(configClass: Class<C>) : SpongeConfigClass<C>(configClass) {

    /**
     * JSON-backed configuration manager for this config type.
     *
     * The manager is created using [SurfConfigApi.createSpongeJsonConfigManager]
     * with [configClass], [configFolder] and [fileName].
     */
    override val manager =
        surfConfigApi.createSpongeJsonConfigManager(configClass, configFolder, fileName)
}