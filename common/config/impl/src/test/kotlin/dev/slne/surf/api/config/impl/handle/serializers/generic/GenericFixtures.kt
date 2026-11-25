package dev.slne.surf.api.config.impl.handle.serializers.generic

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.impl.handle.serializers.Price
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

/**
 * A serializer whose type argument is a type variable, so its type comes from the annotation;
 * writes a [Price] as `<cents>g`.
 */
@SurfConfigSerializer(type = Price::class)
class GenericCentsSerializer<T : Any> : TypeSerializer<T> {

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(type: Type, node: ConfigurationNode): T {
        return Price(node.string.orEmpty().removeSuffix("g").toLong()) as T
    }

    override fun serialize(type: Type, obj: T?, node: ConfigurationNode) {
        node.set("${(obj as Price).cents}g")
    }
}

@ConfigSerializable
@SurfConfig("generic.yml")
data class GenericConfig(val price: Price = Price(4))
