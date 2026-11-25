package dev.slne.surf.api.config.impl.handle.serializers.baseclass

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.impl.handle.serializers.CentsSerializer
import dev.slne.surf.api.config.impl.handle.serializers.Price
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * A bean of a subclass of [CentsSerializer], which is no bean itself.
 */
@SurfConfigSerializer(applyToAll = false)
class SubclassCentsSerializer : CentsSerializer()

@ConfigSerializable
@SurfConfig("base.yml", serializers = [CentsSerializer::class])
data class BaseClassConfig(val price: Price = Price(1))
