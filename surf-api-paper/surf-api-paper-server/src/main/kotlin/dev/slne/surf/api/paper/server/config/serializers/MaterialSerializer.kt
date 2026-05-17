package dev.slne.surf.api.paper.server.config.serializers

import org.bukkit.Material
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

internal object MaterialSerializer : ScalarSerializer.Annotated<Material>(Material::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): Material {
        return Material.matchMaterial(obj.toString()) ?: throw SerializationException(
            Material::class.java,
            "$obj($type) is not a valid material"
        )
    }

    override fun serialize(type: AnnotatedType, item: Material, typeSupported: Predicate<Class<*>>): Any {
        return item.key.toString()
    }
}