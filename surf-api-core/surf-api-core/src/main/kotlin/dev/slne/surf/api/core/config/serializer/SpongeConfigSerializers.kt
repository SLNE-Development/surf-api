package dev.slne.surf.api.core.config.serializer

import dev.slne.surf.api.core.config.constraints.*
import dev.slne.surf.api.core.config.serializer.collection.map.FastutilMapSerializer
import dev.slne.surf.api.core.config.serializer.collection.map.MapSerializer
import dev.slne.surf.api.core.config.type.BooleanOrDefault
import dev.slne.surf.api.core.config.type.ConfigDuration
import dev.slne.surf.api.core.config.type.DurationOrDisabled
import dev.slne.surf.api.core.config.type.StringOrDefault
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
import org.jetbrains.annotations.MustBeInvokedByOverriders
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

    @MustBeInvokedByOverriders
    protected open fun registerDefaults(builder: TypeSerializerCollection.Builder) {
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
        builder.register(ConfigDuration.Serializer)
        builder.register(BooleanOrDefault.Serializer)
        builder.register(StringOrDefault.Serializer)
        builder.register(DurationOrDisabled.Serializer)
        builder.register(IntOr.Default.Serializer)
        builder.register(IntOr.Disabled.Serializer)
        builder.register(DoubleOr.Default.Serializer)
        builder.register(DoubleOr.Disabled.Serializer)
        builder.register(UuidSerializer)
        builder.register(RegexSerializer)
        builder.register(PatternSerializer)
        builder.register(UriSerializer)
        builder.register(UrlSerializer)
        builder.register(PathSerializer)
        builder.register(FileSerializer)
        builder.register(TextColorSerializer)

        //region fastutil maps
        // @formatter:off
        builder.register(object : TypeToken<Reference2BooleanMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2BooleanMap<*>>({ Reference2BooleanOpenHashMap(it as Map<*, Boolean>) }, java.lang.Boolean.TYPE))
        builder.register(object : TypeToken<Reference2ByteMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2ByteMap<*>>({ Reference2ByteOpenHashMap(it as Map<*, Byte>) }, java.lang.Byte.TYPE))
        builder.register(object : TypeToken<Reference2CharMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2CharMap<*>>({ Reference2CharOpenHashMap(it as Map<*, Char>) }, java.lang.Character.TYPE))
        builder.register(object : TypeToken<Reference2DoubleMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2DoubleMap<*>>({ Reference2DoubleOpenHashMap(it as Map<*, Double>) }, java.lang.Double.TYPE))
        builder.register(object : TypeToken<Reference2FloatMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2FloatMap<*>>({ Reference2FloatOpenHashMap(it as Map<*, Float>) }, java.lang.Float.TYPE))
        builder.register(object : TypeToken<Reference2IntMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2IntMap<*>>({ Reference2IntOpenHashMap(it as Map<*, Int>) }, Integer.TYPE))
        builder.register(object : TypeToken<Reference2LongMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2LongMap<*>>({ Reference2LongOpenHashMap(it as Map<*, Long>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Reference2ShortMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Reference2ShortMap<*>>({ Reference2ShortOpenHashMap(it as Map<*, Short>) }, java.lang.Short.TYPE))
        builder.register(object : TypeToken<Reference2ObjectMap<*, *>>() {}, FastutilMapSerializer.SomethingToSomething<Reference2ObjectMap<*, *>>({ Reference2ObjectOpenHashMap(it) }))
        builder.register(object : TypeToken<Int2BooleanMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Int2BooleanMap>({ Int2BooleanOpenHashMap(it as Map<Int, Boolean>) }, Integer.TYPE, java.lang.Boolean.TYPE))
        builder.register(object : TypeToken<Int2ByteMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Int2ByteMap>({ Int2ByteOpenHashMap(it as Map<Int, Byte>) }, Integer.TYPE, java.lang.Byte.TYPE))
        builder.register(object : TypeToken<Int2CharMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Int2CharMap>({ Int2CharOpenHashMap(it as Map<Int, Char>) }, Integer.TYPE, java.lang.Character.TYPE))
        builder.register(object : TypeToken<Int2DoubleMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Int2DoubleMap>({ Int2DoubleOpenHashMap(it as Map<Int, Double>) }, Integer.TYPE, java.lang.Double.TYPE))
        builder.register(object : TypeToken<Int2FloatMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Int2FloatMap>({ Int2FloatOpenHashMap(it as Map<Int, Float>) }, Integer.TYPE, java.lang.Float.TYPE))
        builder.register(object : TypeToken<Int2IntMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Int2IntMap>({ Int2IntOpenHashMap(it as Map<Int, Int>) }, Integer.TYPE, Integer.TYPE))
        builder.register(object : TypeToken<Int2LongMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Int2LongMap>({ Int2LongOpenHashMap(it as Map<Int, Long>) }, Integer.TYPE, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Int2ObjectMap<*>>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2ObjectMap<*>>({ Int2ObjectOpenHashMap(it as Map<Int, *>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2ReferenceMap<*>>() {}, FastutilMapSerializer.PrimitiveToSomething<Int2ReferenceMap<*>>({ Int2ReferenceOpenHashMap(it as Map<Int, *>) }, Integer.TYPE))
        builder.register(object : TypeToken<Int2ShortMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Int2ShortMap>({ Int2ShortOpenHashMap(it as Map<Int, Short>) }, Integer.TYPE, java.lang.Short.TYPE))
        builder.register(object : TypeToken<Long2BooleanMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Long2BooleanMap>({ Long2BooleanOpenHashMap(it as Map<Long, Boolean>) }, java.lang.Long.TYPE, java.lang.Boolean.TYPE))
        builder.register(object : TypeToken<Long2ByteMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Long2ByteMap>({ Long2ByteOpenHashMap(it as Map<Long, Byte>) }, java.lang.Long.TYPE, java.lang.Byte.TYPE))
        builder.register(object : TypeToken<Long2CharMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Long2CharMap>({ Long2CharOpenHashMap(it as Map<Long, Char>) }, java.lang.Long.TYPE, java.lang.Character.TYPE))
        builder.register(object : TypeToken<Long2DoubleMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Long2DoubleMap>({ Long2DoubleOpenHashMap(it as Map<Long, Double>) }, java.lang.Long.TYPE, java.lang.Double.TYPE))
        builder.register(object : TypeToken<Long2FloatMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Long2FloatMap>({ Long2FloatOpenHashMap(it as Map<Long, Float>) }, java.lang.Long.TYPE, java.lang.Float.TYPE))
        builder.register(object : TypeToken<Long2IntMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Long2IntMap>({ Long2IntOpenHashMap(it as Map<Long, Int>) }, java.lang.Long.TYPE, Integer.TYPE))
        builder.register(object : TypeToken<Long2LongMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Long2LongMap>({ Long2LongOpenHashMap(it as Map<Long, Long>) }, java.lang.Long.TYPE, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2ObjectMap<*>>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2ObjectMap<*>>({ Long2ObjectOpenHashMap(it as Map<Long, *>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2ReferenceMap<*>>() {}, FastutilMapSerializer.PrimitiveToSomething<Long2ReferenceMap<*>>({ Long2ReferenceOpenHashMap(it as Map<Long, *>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Long2ShortMap>() {}, FastutilMapSerializer.PrimitiveToPrimitive<Long2ShortMap>({ Long2ShortOpenHashMap(it as Map<Long, Short>) }, java.lang.Long.TYPE, java.lang.Short.TYPE))
        builder.register(object : TypeToken<Object2BooleanMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2BooleanMap<*>>({ Object2BooleanOpenHashMap(it as Map<*, Boolean>) }, java.lang.Boolean.TYPE))
        builder.register(object : TypeToken<Object2ByteMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2ByteMap<*>>({ Object2ByteOpenHashMap(it as Map<*, Byte>) }, java.lang.Byte.TYPE))
        builder.register(object : TypeToken<Object2CharMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2CharMap<*>>({ Object2CharOpenHashMap(it as Map<*, Char>) }, java.lang.Character.TYPE))
        builder.register(object : TypeToken<Object2DoubleMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2DoubleMap<*>>({ Object2DoubleOpenHashMap(it as Map<*, Double>) }, java.lang.Double.TYPE))
        builder.register(object : TypeToken<Object2FloatMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2FloatMap<*>>({ Object2FloatOpenHashMap(it as Map<*, Float>) }, java.lang.Float.TYPE))
        builder.register(object : TypeToken<Object2IntMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2IntMap<*>>({ Object2IntOpenHashMap(it as Map<*, Int>) }, Integer.TYPE))
        builder.register(object : TypeToken<Object2LongMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2LongMap<*>>({ Object2LongOpenHashMap(it as Map<*, Long>) }, java.lang.Long.TYPE))
        builder.register(object : TypeToken<Object2ObjectMap<*, *>>() {}, FastutilMapSerializer.SomethingToSomething<Object2ObjectMap<*, *>>({ Object2ObjectOpenHashMap(it) }))
        builder.register(object : TypeToken<Object2ReferenceMap<*, *>>() {}, FastutilMapSerializer.SomethingToSomething<Object2ReferenceMap<*, *>>({ Object2ReferenceOpenHashMap(it) }))
        builder.register(object : TypeToken<Object2ShortMap<*>>() {}, FastutilMapSerializer.SomethingToPrimitive<Object2ShortMap<*>>({ Object2ShortOpenHashMap(it as Map<*, Short>) }, java.lang.Short.TYPE))
        // @formatter:on
        //endregion

        // register after fastutil specific serializers have been registered
        builder.register(MapSerializer.TYPE, MapSerializer(false))

        builder.registerAnnotatedObjects(
            ObjectMapper.factoryBuilder()
                .addDiscoverer(dataClassFieldDiscoverer())
                .addConstraint(PositiveNumber.Companion.Factory)
                .addConstraint(PositiveNumber.Companion.FactoryIntOr)
                .addConstraint(PositiveNumber.Companion.FactoryDoubleOr)
                .addConstraint(NegativeNumber.Companion.Factory)
                .addConstraint(NegativeNumber.Companion.FactoryIntOr)
                .addConstraint(NegativeNumber.Companion.FactoryDoubleOr)
                .addConstraint(MinNumber.Companion.Factory)
                .addConstraint(MinNumber.Companion.FactoryIntOr)
                .addConstraint(MinNumber.Companion.FactoryDoubleOr)
                .addConstraint(MaxNumber.Companion.Factory)
                .addConstraint(MaxNumber.Companion.FactoryIntOr)
                .addConstraint(MaxNumber.Companion.FactoryDoubleOr)
                .addConstraint(NotBlank.Companion.Factory)
                .addConstraint(Trimmed.Companion.Factory)
                .addConstraint(MaxLength.Companion.Factory)
                .addConstraint(MaxLength.Companion.FactoryStringOrDefault)
                .addConstraint(MinLength.Companion.Factory)
                .addConstraint(MinLength.Companion.FactoryStringOrDefault)
                .addConstraint(StartsWith.Companion.Factory)
                .addConstraint(StartsWith.Companion.FactoryStringOrDefault)
                .addConstraint(EndsWith.Companion.Factory)
                .addConstraint(EndsWith.Companion.FactoryStringOrDefault)
                .addConstraint(Contains.Companion.Factory)
                .addConstraint(Contains.Companion.FactoryStringOrDefault)
                .addConstraint(Range.Companion.Factory)
                .addConstraint(Range.Companion.FactoryIntOr)
                .addConstraint(Range.Companion.FactoryDoubleOr)
                .addConstraint(MinSize.Companion.Factory)
                .addConstraint(MaxSize.Companion.Factory)
                .addConstraint(NotEmpty.Companion.Factory)
                .addConstraint(NoDuplicates.Companion.Factory)
                .addConstraint(MinDuration.Companion.Factory)
                .addConstraint(MinDuration.Companion.FactoryDurationOrDisabled)
                .addConstraint(MaxDuration.Companion.Factory)
                .addConstraint(MaxDuration.Companion.FactoryDurationOrDisabled)
                .addConstraint(DisallowValues.Companion.Factory)
                .addConstraint(Namespace.Companion.Factory)
                .addConstraint(ExistingFile.Companion.Factory)
                .addConstraint(Directory.Companion.Factory)
                .addConstraint(WritablePath.Companion.Factory)
                .build()
        )

        registerDefaults(builder)
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
