package dev.slne.surf.api.core.config.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.function.Predicate

internal object PathSerializer : ScalarSerializer.Annotated<Path>(Path::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): Path {
        try {
            return Path.of(obj.toString())
        } catch (e: InvalidPathException) {
            throw SerializationException(Path::class.java, "$obj($type) is not a valid path", e)
        }
    }

    override fun serialize(type: AnnotatedType, item: Path, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
}