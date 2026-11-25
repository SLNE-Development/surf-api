package dev.slne.surf.api.config

import dev.slne.surf.api.config.manager.SpongeConfigHandle
import dev.slne.surf.api.config.manager.SpongeConfigManager
import dev.slne.surf.api.config.migration.ConfigMigrations
import dev.slne.surf.api.config.serializer.ConfigSerializerCollections
import org.spongepowered.configurate.serialize.TypeSerializer
import java.nio.file.Path

/**
 * Opens configuration files at any path, outside the [SurfConfig] integration.
 *
 * For files whose location is only known at runtime, files read before a plugin's context
 * exists, and libraries that are not beans:
 *
 * ```kotlin
 * ConfigHandles.open<ReportConfig>(folder.resolve("reports.yml")).use { handle ->
 *     render(handle.value)
 * }
 * ```
 *
 * The caller owns the returned handle and closes it once it no longer needs the file. Opening
 * reads the default serializers of surf-api's root context, so surf-api's runtime must be running.
 * The `applyToAll` serializers of plugins never apply to these files.
 */
object ConfigHandles {

    /**
     * Opens the file at [path] as [configClass], applying [migrations] and writing the file back
     * with the defaults of [configClass].
     *
     * The folder of [path] is created if missing.
     *
     * @param configClass the configuration class
     * @param path the file
     * @param format the format of the file; `null` takes it from the extension of [path], JSON for
     * `.json` and YAML for any other
     * @param migrations the migrations to apply on open and on every reload
     * @param serializers serializers that apply before surf-api's root defaults, each to the type
     * its `TypeSerializer` supertype or `ScalarSerializer.type()` declares
     * @return an open handle owned by the caller
     * @throws dev.slne.surf.api.config.manager.LoadConfigException if the file cannot be read,
     * migrated or written, or if [configClass] carries Configurate annotations that were not
     * relocated like surf-api's Configurate
     * @throws dev.slne.surf.api.config.manager.SerializationConfigException if the file does not
     * match [configClass]
     * @throws IllegalArgumentException if the type of a serializer in [serializers] cannot be
     * resolved, or two of them serialize the same type
     * @throws IllegalStateException if surf-api's root context has not been started
     */
    fun <C : Any> open(
        configClass: Class<C>,
        path: Path,
        format: ConfigFormat? = null,
        migrations: ConfigMigrations<C> = ConfigMigrations.none(),
        serializers: List<TypeSerializer<*>> = emptyList(),
    ): ConfigHandle<C> {
        val collection = ConfigSerializerCollections.instance.withRootDefaults(serializers)
        val resolvedFormat = format ?: ConfigFormat.ofPath(path) ?: ConfigFormat.YAML
        val handle = SpongeConfigHandle(configClass) {
            SpongeConfigManager.open(configClass, path, resolvedFormat, migrations, collection)
        }
        handle.load()

        return handle
    }

    /**
     * Opens the file at [path] as [C], applying [migrations] and writing the file back with the
     * defaults of [C].
     *
     * @param path the file
     * @param format the format of the file; `null` takes it from the extension of [path], JSON for
     * `.json` and YAML for any other
     * @param migrations the migrations to apply on open and on every reload
     * @param serializers serializers that apply before surf-api's root defaults
     * @return an open handle owned by the caller
     * @see open
     */
    inline fun <reified C : Any> open(
        path: Path,
        format: ConfigFormat? = null,
        migrations: ConfigMigrations<C> = ConfigMigrations.none(),
        serializers: List<TypeSerializer<*>> = emptyList(),
    ): ConfigHandle<C> {
        return open(C::class.java, path, format, migrations, serializers)
    }
}
