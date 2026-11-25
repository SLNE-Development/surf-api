package dev.slne.surf.api.config.impl.handle.serializers.pluginb

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.impl.handle.serializers.Price
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * A config of plugin B, which declares no serializer of its own.
 */
@ConfigSerializable
@SurfConfig("b.yml")
data class PluginBConfig(val price: Price = Price(5))
