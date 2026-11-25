package dev.slne.standalone.samename

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.*
import java.util.function.Predicate

/**
 * Shares its simple name with surf-api's root default for [UUID]; writes upper case.
 */
@SurfConfigSerializer(applyToAll = false)
class UuidSerializer : ScalarSerializer<UUID>(UUID::class.java) {

    override fun deserialize(type: Type, obj: Any): UUID = UUID.fromString(obj.toString())

    override fun serialize(item: UUID, typeSupported: Predicate<Class<*>>): Any {
        return item.toString().uppercase()
    }
}

@ConfigSerializable
@SurfConfig("same-name.yml", serializers = [UuidSerializer::class])
data class SameNameConfig(val id: UUID = SAME_NAME_ID)

val SAME_NAME_ID: UUID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e")
