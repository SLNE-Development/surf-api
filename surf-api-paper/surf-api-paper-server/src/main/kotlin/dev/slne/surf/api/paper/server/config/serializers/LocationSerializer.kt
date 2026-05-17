package dev.slne.surf.api.paper.server.config.serializers

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Location
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

internal object LocationSerializer : TypeSerializer<Location> {
    override fun deserialize(type: Type, node: ConfigurationNode): Location {
        val worldKey = node.node("world-key").get(Key::class.java)
            ?: throw SerializationException(Location::class.java, "Location is missing world")

        val world = Bukkit.getWorld(worldKey)
            ?: throw SerializationException(Location::class.java, "Unknown world: $worldKey")

        return Location(
            world,
            node.node("x").double,
            node.node("y").double,
            node.node("z").double,
            node.node("yaw").float,
            node.node("pitch").float
        )
    }

    override fun serialize(type: Type, obj: Location?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }

        node.node("world").set(obj.world.key)
        node.node("x").set(obj.x)
        node.node("y").set(obj.y)
        node.node("z").set(obj.z)
        node.node("yaw").set(obj.yaw)
        node.node("pitch").set(obj.pitch)
    }
}