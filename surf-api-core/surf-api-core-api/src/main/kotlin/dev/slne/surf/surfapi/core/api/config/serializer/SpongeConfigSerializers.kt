package dev.slne.surf.surfapi.core.api.config.serializer

import dev.slne.surf.surfapi.core.api.config.manager.PreferUsingSpongeConfigOverDazzlConf
import dev.slne.surf.surfapi.core.api.minimessage.SurfMiniMessageHolder
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.serialize.AbstractListChildSerializer
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.util.CheckedConsumer
import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.AnnotatedType
import java.lang.reflect.Type
import java.util.*
import java.util.function.Consumer

/**
 * Serializers for Sponge configurations, including support for Adventure [Component] and other types.
 */
object SpongeConfigSerializers {

    /**
     * Registers custom serializers with the provided builder.
     */
    var SERIALIZERS: Consumer<TypeSerializerCollection.Builder> = Consumer { builder ->
        builder.register(Component::class.java, ComponentSerializer())
        builder.register(LinkedListSerializer.TYPE, LinkedListSerializer())
        builder.registerAnnotatedObjects(objectMapperFactory())
    }

    /**
     * Serializer for [Component] objects in Sponge configurations.
     */
    class ComponentSerializer : TypeSerializer<Component> {

        @OptIn(PreferUsingSpongeConfigOverDazzlConf::class)
        override fun deserialize(type: Type?, node: ConfigurationNode): Component {
            val message = node.string ?: return Component.empty()

            return SurfMiniMessageHolder.miniMessage().deserialize(message)
        }

        @OptIn(PreferUsingSpongeConfigOverDazzlConf::class)
        override fun serialize(type: Type?, obj: Component?, node: ConfigurationNode) {
            if (obj == null) {
                return
            }

            node.set(SurfMiniMessageHolder.miniMessage().serialize(obj))
        }
    }

    /**
     * Serializer for [LinkedList] objects in Sponge configurations.
     */
    class LinkedListSerializer : AbstractListChildSerializer<LinkedList<Any>>() {

        override fun elementType(containerType: AnnotatedType): AnnotatedType? {
            if (containerType !is AnnotatedParameterizedType) {
                throw SerializationException(containerType, "Raw types are not supported for collections")
            }

            return containerType.annotatedActualTypeArguments[0]
        }

        override fun createNew(length: Int, elementType: AnnotatedType?): LinkedList<Any> {
            return LinkedList<Any>()
        }

        @Throws(SerializationException::class)
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

        companion object {
            val TYPE: TypeToken<LinkedList<Any>> = object : TypeToken<LinkedList<Any>>() {}
        }
    }
}
