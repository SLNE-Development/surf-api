package dev.slne.surf.api.core.config.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.*
import java.util.function.Predicate

internal object UuidSerializer : ScalarSerializer.Annotated<UUID>(UUID::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): UUID {
        return try {
            UUID.fromString(obj.toString())
        } catch (e: IllegalArgumentException) {
            throw SerializationException(UUID::class.java, "$obj($type) is not a valid UUID", e)
        }
    }

    override fun serialize(type: AnnotatedType, item: UUID, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
}