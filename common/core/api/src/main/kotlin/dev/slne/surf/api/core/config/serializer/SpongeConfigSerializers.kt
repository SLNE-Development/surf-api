package dev.slne.surf.api.core.config.serializer

import dev.slne.surf.api.core.config.constraints.*
import dev.slne.surf.api.core.config.serializer.collection.map.MapSerializer
import dev.slne.surf.api.core.config.type.BooleanOrDefault
import dev.slne.surf.api.core.config.type.ConfigDuration
import dev.slne.surf.api.core.config.type.DurationOrDisabled
import dev.slne.surf.api.core.config.type.StringOrDefault
import dev.slne.surf.api.core.config.type.number.DoubleOr
import dev.slne.surf.api.core.config.type.number.IntOr
import dev.slne.surf.api.core.minimessage.SurfMiniMessageHolder
import dev.slne.surf.api.core.util.requiredService
import io.leangen.geantyref.TypeToken
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
    private val _typeTokenSerializers = mutableMapOf<TypeToken<*>, TypeSerializer<*>>()
    val typeTokenSerializers: Map<TypeToken<*>, TypeSerializer<*>> get() = _typeTokenSerializers

    private val _classSerializers = mutableMapOf<Class<*>, TypeSerializer<*>>()
    val classSerializers: Map<Class<*>, TypeSerializer<*>> get() = _classSerializers

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
