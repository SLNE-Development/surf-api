package dev.slne.surf.api.core.config.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import java.io.File
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

internal object FileSerializer : ScalarSerializer.Annotated<File>(File::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): File {
        return File(obj.toString())
    }

    override fun serialize(type: AnnotatedType, item: File, typeSupported: Predicate<Class<*>>): Any {
        return item.path
    }
}