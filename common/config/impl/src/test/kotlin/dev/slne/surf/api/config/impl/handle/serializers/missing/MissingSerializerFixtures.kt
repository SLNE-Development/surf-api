package dev.slne.surf.api.config.impl.handle.serializers.missing

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.impl.handle.serializers.CentsSerializer
import dev.slne.surf.api.config.impl.handle.serializers.Price
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * A serializer that is no bean.
 */
class UnmanagedCentsSerializer : CentsSerializer()

@ConfigSerializable
@SurfConfig("missing.yml", lazy = true, serializers = [UnmanagedCentsSerializer::class])
data class MissingSerializerConfig(val price: Price = Price(1))
