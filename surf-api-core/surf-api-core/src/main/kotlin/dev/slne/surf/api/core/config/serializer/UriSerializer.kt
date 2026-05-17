package dev.slne.surf.api.core.config.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.net.URI
import java.net.URISyntaxException
import java.util.function.Predicate

internal object UriSerializer : ScalarSerializer.Annotated<URI>(URI::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): URI {
        return try {
            URI(obj.toString())
        } catch (e: URISyntaxException) {
            throw SerializationException(URI::class.java, "$obj($type) is not a valid URI", e)
        }
    }

    override fun serialize(type: AnnotatedType, item: URI, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
}