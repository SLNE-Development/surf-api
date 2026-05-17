package dev.slne.surf.api.core.config.serializer

import dev.slne.surf.api.core.config.constraints.MaxNumber
import dev.slne.surf.api.core.config.constraints.MinNumber
import dev.slne.surf.api.core.config.constraints.PositiveNumber
import dev.slne.surf.api.core.config.serializer.collection.map.FastutilMapSerializer
import dev.slne.surf.api.core.config.serializer.collection.map.MapSerializer
import dev.slne.surf.api.core.config.type.BooleanOrDefault
import dev.slne.surf.api.core.config.type.DurationOrDisabled
import dev.slne.surf.api.core.config.type.number.DoubleOr
import dev.slne.surf.api.core.config.type.number.IntOr
import dev.slne.surf.api.core.minimessage.SurfMiniMessageHolder
import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.core.util.requiredService
import io.leangen.geantyref.TypeToken
import it.unimi.dsi.fastutil.ints.*
import it.unimi.dsi.fastutil.longs.*
import it.unimi.dsi.fastutil.objects.*
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.kotlin.extensions.addConstraint
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.*
import org.spongepowered.configurate.util.CheckedConsumer
import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.AnnotatedType
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate

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

        builder.register(ComponentSerializer())
        builder.register(EnumValueSerializer)
        builder.register(KeySerializer)
        builder.register(DurationSerializer)
        builder.register(BooleanOrDefault.Serializer)
        builder.register(DurationOrDisabled.Serializer)
        builder.register(IntOr.Default.Serializer)
        builder.register(IntOr.Disabled.Serializer)
        builder.register(DoubleOr.Default.Serializer)
        builder.register(DoubleOr.Disabled.Serializer)
        builder.register(MapSerializer.TYPE, MapSerializer(false))

        //region fastutil maps
        // @formatter:off
        builder.register(object : TypeToken<Reference2BooleanMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2BooleanMap<Any>>({ Reference2BooleanOpenHashMap(it as Map<out Any, Boolean>) }, java.lang.Boolean.TYPE))
        builder.register(object : TypeToken<Reference2ByteMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2ByteMap<Any>>({ Reference2ByteOpenHashMap(it as Map<out Any, Byte>) }, java.lang.Byte.TYPE))
        builder.register(object : TypeToken<Reference2CharMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2CharMap<Any>>({ Reference2CharOpenHashMap(it as Map<out Any, Char>) }, java.lang.Character.TYPE))
        builder.register(object : TypeToken<Reference2DoubleMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2DoubleMap<Any>>({ Reference2DoubleOpenHashMap(it as Map<out Any, Double>) }, java.lang.Double.TYPE))
        builder.register(object : TypeToken<Reference2FloatMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2FloatMap<Any>>({ Reference2FloatOpenHashMap(it as Map<out Any, Float>) }, java.lang.Float.TYPE))
        builder.register(object : TypeToken<Reference2IntMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2IntMap<Any>>({ Reference2IntOpenHashMap(it as Map<out Any, Int>) }, Integer.TYPE))
        builder.register(object : TypeToken<Reference2LongMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2LongMap<Any>>({ Reference2LongOpenHashMap(it as Map<out Any, Long>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Reference2ShortMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2ShortMap<Any>>({ Reference2ShortOpenHashMap(it as Map<out Any, Short>) }, java.lang.Short.TYPE))
        builder.register(object : TypeToken<Reference2ObjectMap<Any, Any>>() {}, FastutilMapSerializer.SomethingToSomething<Reference2ObjectMap<Any, Any>>({ Reference2ObjectOpenHashMap(it) }))
        builder.register(object : TypeToken<Int2BooleanMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2BooleanMap>({ Int2BooleanOpenHashMap(it as Map<Int, Boolean>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2ByteMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2ByteMap>({ Int2ByteOpenHashMap(it as Map<Int, Byte>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2CharMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2CharMap>({ Int2CharOpenHashMap(it as Map<Int, Char>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2DoubleMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2DoubleMap>({ Int2DoubleOpenHashMap(it as Map<Int, Double>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2FloatMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2FloatMap>({ Int2FloatOpenHashMap(it as Map<Int, Float>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2IntMap>() {}, FastutilMapSerializer.SomethingToSomething<Int2IntMap>({ Int2IntOpenHashMap(it as Map<Int, Int>) }))
        builder.register(object : TypeToken<Int2LongMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2LongMap>({ Int2LongOpenHashMap(it as Map<Int, Long>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2ObjectMap<Any>>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2ObjectMap<Any>>({ Int2ObjectOpenHashMap(it as Map<Int, Any>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2ReferenceMap<Any>>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2ReferenceMap<Any>>({ Int2ReferenceOpenHashMap(it as Map<Int, Any>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2ShortMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2ShortMap>({ Int2ShortOpenHashMap(it as Map<Int, Short>) }, Integer.TYPE))
        builder.register(object : TypeToken<Long2BooleanMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2BooleanMap>({ Long2BooleanOpenHashMap(it as Map<Long, Boolean>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2ByteMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2ByteMap>({ Long2ByteOpenHashMap(it as Map<Long, Byte>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2CharMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2CharMap>({ Long2CharOpenHashMap(it as Map<Long, Char>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2DoubleMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2DoubleMap>({ Long2DoubleOpenHashMap(it as Map<Long, Double>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2FloatMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2FloatMap>({ Long2FloatOpenHashMap(it as Map<Long, Float>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2IntMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2IntMap>({ Long2IntOpenHashMap(it as Map<Long, Int>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2LongMap>() {}, FastutilMapSerializer.SomethingToSomething<Long2LongMap>({ Long2LongOpenHashMap(it as Map<Long, Long>) }))
        builder.register(object : TypeToken<Long2ObjectMap<Any>>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2ObjectMap<Any>>({ Long2ObjectOpenHashMap(it as Map<Long, Any>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2ReferenceMap<Any>>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2ReferenceMap<Any>>({ Long2ReferenceOpenHashMap(it as Map<Long, Any>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2ShortMap>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2ShortMap>({ Long2ShortOpenHashMap(it as Map<Long, Short>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Object2BooleanMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2BooleanMap<Any>>({ Object2BooleanOpenHashMap(it as Map<out Any, Boolean>) }, java.lang.Boolean.TYPE))
        builder.register(object : TypeToken<Object2ByteMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2ByteMap<Any>>({ Object2ByteOpenHashMap(it as Map<out Any, Byte>) }, java.lang.Byte.TYPE))
        builder.register(object : TypeToken<Object2CharMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2CharMap<Any>>({ Object2CharOpenHashMap(it as Map<out Any, Char>) }, java.lang.Character.TYPE))
        builder.register(object : TypeToken<Object2DoubleMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2DoubleMap<Any>>({ Object2DoubleOpenHashMap(it as Map<out Any, Double>) }, java.lang.Double.TYPE))
        builder.register(object : TypeToken<Object2FloatMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2FloatMap<Any>>({ Object2FloatOpenHashMap(it as Map<out Any, Float>) }, java.lang.Float.TYPE))
        builder.register(object : TypeToken<Object2IntMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2IntMap<Any>>({ Object2IntOpenHashMap(it as Map<out Any, Int>) }, Integer.TYPE))
        builder.register(object : TypeToken<Object2LongMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2LongMap<Any>>({ Object2LongOpenHashMap(it as Map<out Any, Long>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Object2ObjectMap<Any, Any>>() {}, FastutilMapSerializer.SomethingToSomething<Object2ObjectMap<Any, Any>>({ Object2ObjectOpenHashMap(it) }))
        builder.register(object : TypeToken<Object2ReferenceMap<Any, Any>>() {}, FastutilMapSerializer.SomethingToSomething<Object2ReferenceMap<Any, Any>>({ Object2ReferenceOpenHashMap(it) }))
        builder.register(object : TypeToken<Object2ShortMap<Any>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2ShortMap<Any>>({ Object2ShortOpenHashMap(it as Map<out Any, Short>) }, java.lang.Short.TYPE))
        // @formatter:on
        //endregion

        builder.registerAnnotatedObjects(
            ObjectMapper.factoryBuilder()
                .addDiscoverer(dataClassFieldDiscoverer())
                .addConstraint(PositiveNumber.Companion.Factory)
                .addConstraint(MinNumber.Companion.Factory)
                .addConstraint(MaxNumber.Companion.Factory)
                .build()
        )
    }

    /**
     * Configurate scalar serializer for Adventure [Component] values.
     *
     * Components are serialized as MiniMessage strings and deserialized through the shared
     * Surf MiniMessage instance.
     */
    class ComponentSerializer : ScalarSerializer.Annotated<Component>(Component::class.java) {

        /**
         * Deserializes a MiniMessage string into a [Component].
         */
        override fun deserialize(type: AnnotatedType, obj: Any): Component {
            return SurfMiniMessageHolder.miniMessage().deserialize(obj.toString())
        }

        /**
         * Serializes [item] into a MiniMessage string.
         */
        override fun serialize(type: AnnotatedType, item: Component, typeSupported: Predicate<Class<*>>): Any {
            return SurfMiniMessageHolder.miniMessage().serialize(item)
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
