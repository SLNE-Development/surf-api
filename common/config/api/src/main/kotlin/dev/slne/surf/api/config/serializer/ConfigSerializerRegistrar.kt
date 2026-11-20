package dev.slne.surf.api.config.serializer

/**
 * Registers a plugin's own Configurate serializers before the plugin's config files are read.
 *
 * Declare it as a bean of the plugin. surf-api calls every registrar of the plugin's context once
 * while the context starts, at the latest before the first `ConfigHandle` of the plugin opens its
 * file, so the plugin's `@SurfConfig` classes can use the registered types. The registrars also run
 * when the plugin declares no `@SurfConfig` class. The registry is shared by all plugins, so
 * configs read through a `SpongeYmlConfigClass` or `SpongeJsonConfigClass` companion see the
 * registered types once the plugin's context has started.
 *
 * ```kotlin
 * @Component
 * class ShopSerializers : ConfigSerializerRegistrar {
 *     override fun register(serializers: SpongeConfigSerializers) {
 *         serializers.registerClassSerializer<Price>(PriceSerializer)
 *     }
 * }
 * ```
 */
fun interface ConfigSerializerRegistrar {

    /**
     * Registers serializers with [serializers].
     */
    fun register(serializers: SpongeConfigSerializers)
}
