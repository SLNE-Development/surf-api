package dev.slne.surf.api.config.impl.handle.serializers.plugina

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.impl.handle.serializers.CentsSerializer
import dev.slne.surf.api.config.impl.handle.serializers.Price
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * Applies to every config of plugin A.
 */
@SurfConfigSerializer
class PluginACentsSerializer : CentsSerializer()

@ConfigSerializable
@SurfConfig("a.yml")
data class PluginAConfig(val price: Price = Price(5))
