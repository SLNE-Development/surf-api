package dev.slne.surf.api.config.impl.handle.serializers.override

import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.config.serializer.SurfConfigSerializer
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.*
import java.util.function.Predicate

/**
 * Replaces the root default for [UUID] in this plugin, writing upper case.
 */
@SurfConfigSerializer
class UpperCaseUuidSerializer : ScalarSerializer<UUID>(UUID::class.java) {

    override fun deserialize(type: Type, obj: Any): UUID = UUID.fromString(obj.toString())

    override fun serialize(item: UUID, typeSupported: Predicate<Class<*>>): Any {
        return item.toString().uppercase()
    }
}

@ConfigSerializable
@SurfConfig("ids.yml")
data class IdConfig(val id: UUID = FIXED_ID)

val FIXED_ID: UUID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e")
