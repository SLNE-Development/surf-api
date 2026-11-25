package dev.slne.surf.api.config.impl.handle.serializers

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

/**
 * A value that Configurate writes as a section unless a serializer writes it as a scalar.
 */
@ConfigSerializable
data class Price(val cents: Long = 0)

/**
 * Writes a [Price] as `<cents>c`; the plugins below declare it as a bean in their own ways.
 */
abstract class CentsSerializer : ScalarSerializer<Price>(Price::class.java) {

    override fun deserialize(type: Type, obj: Any): Price {
        return Price(obj.toString().removeSuffix("c").toLong())
    }

    override fun serialize(item: Price, typeSupported: Predicate<Class<*>>): Any {
        return "${item.cents}c"
    }
}
