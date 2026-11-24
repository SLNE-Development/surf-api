package dev.slne.surf.api.config.serializer

import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.serialize.AbstractListChildSerializer
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.util.CheckedConsumer
import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.AnnotatedType
import java.util.*

/**
 * Serializer for [java.util.LinkedList] objects in Sponge configurations.
 */
internal object LinkedListSerializer : AbstractListChildSerializer<LinkedList<Any>>() {
    val TYPE: TypeToken<LinkedList<Any>> = object : TypeToken<LinkedList<Any>>() {}

    override fun elementType(containerType: AnnotatedType): AnnotatedType? {
        if (containerType !is AnnotatedParameterizedType) {
            throw SerializationException(
                containerType,
                "Raw types are not supported for collections",
            )
        }

        return containerType.annotatedActualTypeArguments[0]
    }

    override fun createNew(length: Int, elementType: AnnotatedType?): LinkedList<Any> {
        return LinkedList<Any>()
    }

    override fun forEachElement(
        collection: LinkedList<Any>,
        action: CheckedConsumer<Any?, SerializationException?>,
    ) {
        for (el in collection) {
            action.accept(el)
        }
    }

    override fun deserializeSingle(
        index: Int, collection: LinkedList<Any>,
        deserialized: Any?,
    ) {
        if (deserialized == null) return

        collection.add(deserialized)
    }
}