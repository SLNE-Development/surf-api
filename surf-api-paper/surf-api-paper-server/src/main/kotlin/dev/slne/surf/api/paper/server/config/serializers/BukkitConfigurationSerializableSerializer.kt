package dev.slne.surf.api.paper.server.config.serializers

import io.leangen.geantyref.GenericTypeReflector
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object BukkitConfigurationSerializableSerializer : TypeSerializer<ConfigurationSerializable> {
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(type: Type, node: ConfigurationNode): ConfigurationSerializable {
        val clazz = GenericTypeReflector.erase(type) as Class<ConfigurationSerializable>
        val args = node.childrenMap().mapValues { it.value.raw() }.mapKeys { it.key.toString() }

        try {
            return ConfigurationSerialization.deserializeObject(args, clazz)
                ?: throw SerializationException(
                    clazz,
                    "Could not deserialize ${clazz.name} via ConfigurationSerializable"
                )
        } catch (e: Throwable) {
            if (e is SerializationException) throw e
            throw SerializationException(clazz, "Could not deserialize ${clazz.name} via ConfigurationSerializable", e)
        }
    }


    @Suppress("OverrideOnly")
    override fun serialize(type: Type, obj: ConfigurationSerializable?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }

        val map = obj.serialize()
        map.forEach { (key, value) ->
            node.node(key).set(value)
        }
    }
}