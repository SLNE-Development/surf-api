package dev.slne.surf.api.core.config.serializer

import dev.slne.surf.api.core.minimessage.SurfMiniMessageHolder
import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.core.util.requiredService
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

val surfSpongeConfigSerializers = requiredService<SpongeConfigSerializers>()

/**
 * Serializers for Sponge configurations, including support for Adventure [Component] and other types.
 */
abstract class SpongeConfigSerializers {
    private val _typeTokenSerializers = mutableObject2ObjectMapOf<TypeToken<*>, TypeSerializer<*>>()
    val typeTokenSerializers get() = _typeTokenSerializers.freeze()

    private val _classSerializers = mutableObject2ObjectMapOf<Class<*>, TypeSerializer<*>>()
    val classSerializers get() = _classSerializers.freeze()

    init {
        registerClassSerializer(Component::class.java, ComponentSerializer())
        registerTypeTokenSerializer(LinkedListSerializer.TYPE, LinkedListSerializer())
    }

    fun <T : Any> registerClassSerializer(clazz: Class<T>, serializer: TypeSerializer<T>) {
        _classSerializers[clazz] = serializer
    }

    inline fun <reified T : Any> registerClassSerializer(serializer: TypeSerializer<T>) {
        registerClassSerializer(T::class.java, serializer)
    }

    fun unregisterSerializer(clazz: Class<*>) {
        _classSerializers.remove(clazz)
    }

    fun registerTypeTokenSerializer(typeToken: TypeToken<*>, serializer: TypeSerializer<*>) {
        _typeTokenSerializers[typeToken] = serializer
    }

    fun unregisterTypeTokenSerializer(typeToken: TypeToken<*>) {
        _typeTokenSerializers.remove(typeToken)
    }

    /**
     * Registers custom serializers with the provided builder.
     */
    @Suppress("UNCHECKED_CAST")
    fun buildSerializersModule(): Consumer<TypeSerializerCollection.Builder> = Consumer { builder ->
        _classSerializers.forEach { (clazz, serializer) ->
            builder.register(clazz as Class<Any>, serializer as TypeSerializer<Any>)
        }

        _typeTokenSerializers.forEach { (type, serializer) ->
            builder.register(type as TypeToken<Any>, serializer as TypeSerializer<Any>)
        }

        builder.registerAnnotatedObjects(objectMapperFactory())
    }

    /**
     * Serializer for [Component] objects in Sponge configurations.
     */
    class ComponentSerializer : TypeSerializer<Component> {
        override fun deserialize(type: Type?, node: ConfigurationNode): Component {
            val message = node.string ?: return Component.empty()

            return SurfMiniMessageHolder.miniMessage().deserialize(message)
        }

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
                throw SerializationException(
                    containerType,
                    "Raw types are not supported for collections"
                )
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
