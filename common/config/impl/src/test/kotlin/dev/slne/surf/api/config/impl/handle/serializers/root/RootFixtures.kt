package dev.slne.surf.api.config.impl.handle.serializers.root

import dev.slne.surf.api.config.impl.handle.serializers.Price
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

/**
 * A root serializer that applies only to the configs that name it; writes a [Price] as
 * `root:<cents>`.
 */
@SurfConfigSerializer(applyToAll = false)
class RootCentsSerializer : ScalarSerializer<Price>(Price::class.java) {

    override fun deserialize(type: Type, obj: Any): Price {
        return Price(obj.toString().removePrefix("root:").toLong())
    }

    override fun serialize(item: Price, typeSupported: Predicate<Class<*>>): Any {
        return "root:${item.cents}"
    }
}
