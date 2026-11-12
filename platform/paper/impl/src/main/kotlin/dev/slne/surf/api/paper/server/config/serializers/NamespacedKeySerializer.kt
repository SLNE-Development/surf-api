package dev.slne.surf.api.paper.server.config.serializers

import org.bukkit.NamespacedKey
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

internal object NamespacedKeySerializer : ScalarSerializer.Annotated<NamespacedKey>(NamespacedKey::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): NamespacedKey {
        return NamespacedKey.fromString(obj.toString())
            ?: throw SerializationException(NamespacedKey::class.java, "$obj($type) is not a valid NamespacedKey")
    }

    override fun serialize(type: AnnotatedType, item: NamespacedKey, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
}