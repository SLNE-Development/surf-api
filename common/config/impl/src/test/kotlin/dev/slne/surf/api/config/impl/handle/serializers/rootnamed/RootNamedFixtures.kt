package dev.slne.surf.api.config.impl.handle.serializers.rootnamed

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.impl.handle.serializers.Price
import dev.slne.surf.api.config.impl.handle.serializers.root.RootCentsSerializer
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
@SurfConfig("root-named.yml", serializers = [RootCentsSerializer::class])
data class RootNamedConfig(val price: Price = Price(3))
