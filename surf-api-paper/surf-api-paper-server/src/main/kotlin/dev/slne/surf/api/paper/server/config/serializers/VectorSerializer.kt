package dev.slne.surf.api.paper.server.config.serializers

import org.bukkit.util.Vector
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

internal object VectorSerializer : TypeSerializer<Vector> {
    override fun deserialize(type: Type, node: ConfigurationNode): Vector {
        return Vector(
            node.node("x").double,
            node.node("y").double,
            node.node("z").double
        )
    }

    override fun serialize(type: Type, obj: Vector?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }

        node.node("x").set(obj.x)
        node.node("y").set(obj.y)
        node.node("z").set(obj.z)
    }
}