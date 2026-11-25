package dev.slne.surf.api.config.impl.handle.serializers.priority

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.impl.handle.serializers.CentsSerializer
import dev.slne.surf.api.config.impl.handle.serializers.Price
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

/**
 * Applies to every config of the plugin.
 */
@SurfConfigSerializer
class PluginWideCentsSerializer : CentsSerializer()

/**
 * Writes a [Price] as `<cents> cents`; applies only where it is named.
 */
@SurfConfigSerializer(applyToAll = false)
class SpelledCentsSerializer : ScalarSerializer<Price>(Price::class.java) {

    override fun deserialize(type: Type, obj: Any): Price {
        return Price(obj.toString().removeSuffix(" cents").toLong())
    }

    override fun serialize(item: Price, typeSupported: Predicate<Class<*>>): Any {
        return "${item.cents} cents"
    }
}

@ConfigSerializable
@SurfConfig("spelled.yml", serializers = [SpelledCentsSerializer::class])
data class SpelledConfig(val price: Price = Price(9))
