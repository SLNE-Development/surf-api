package dev.slne.surf.api.config.impl.handle.serializers.explicit

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.impl.handle.serializers.CentsSerializer
import dev.slne.surf.api.config.impl.handle.serializers.Price
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * Applies only to the configs that name it.
 */
@SurfConfigSerializer(applyToAll = false)
class ExplicitCentsSerializer : CentsSerializer()

@ConfigSerializable
@SurfConfig("named.yml", serializers = [ExplicitCentsSerializer::class])
data class NamingConfig(val price: Price = Price(7))

@ConfigSerializable
@SurfConfig("unnamed.yml")
data class NotNamingConfig(val price: Price = Price(7))
