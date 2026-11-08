package dev.slne.surf.api.core.config.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.net.MalformedURLException
import java.net.URL
import java.util.function.Predicate

@Suppress("DEPRECATION")
internal object UrlSerializer : ScalarSerializer.Annotated<URL>(URL::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): URL {
        return try {
            URL(obj.toString())
        } catch (e: MalformedURLException) {
            throw SerializationException(URL::class.java, "$obj($type) is not a valid URL", e)
        }
    }

    override fun serialize(type: AnnotatedType, item: URL, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
}