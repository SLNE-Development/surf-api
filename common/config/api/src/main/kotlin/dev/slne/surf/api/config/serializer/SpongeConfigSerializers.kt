package dev.slne.surf.api.config.serializer

import dev.slne.surf.api.config.constraints.*
import dev.slne.surf.api.config.serializer.collection.map.MapSerializer
import dev.slne.surf.api.config.type.BooleanOrDefault
import dev.slne.surf.api.config.type.ConfigDuration
import dev.slne.surf.api.config.type.DurationOrDisabled
import dev.slne.surf.api.config.type.StringOrDefault
import dev.slne.surf.api.config.type.number.DoubleOr
import dev.slne.surf.api.config.type.number.IntOr
import dev.slne.surf.api.spring.surfBean
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.text.Component
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.kotlin.extensions.addConstraint
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

internal val surfSpongeConfigSerializers by surfBean<SpongeConfigSerializers>()

/**
 * Serializers for Sponge configurations, including support for Adventure [Component] and other types.
 */
abstract class SpongeConfigSerializers {
    val typeTokenSerializers: Map<TypeToken<*>, TypeSerializer<*>>
        field = ConcurrentHashMap<TypeToken<*>, TypeSerializer<*>>()

    val classSerializers: Map<Class<*>, TypeSerializer<*>>
        field = ConcurrentHashMap<Class<*>, TypeSerializer<*>>()

    fun <T : Any> registerClassSerializer(clazz: Class<T>, serializer: TypeSerializer<T>) {
        classSerializers[clazz] = serializer
    }

    inline fun <reified T : Any> registerClassSerializer(serializer: TypeSerializer<T>) {
        registerClassSerializer(T::class.java, serializer)
    }

    fun unregisterSerializer(clazz: Class<*>) {
        classSerializers.remove(clazz)
    }

    fun registerTypeTokenSerializer(typeToken: TypeToken<*>, serializer: TypeSerializer<*>) {
        typeTokenSerializers[typeToken] = serializer
    }

    fun unregisterTypeTokenSerializer(typeToken: TypeToken<*>) {
        typeTokenSerializers.remove(typeToken)
    }

    @MustBeInvokedByOverriders
    protected open fun registerDefaults(builder: TypeSerializerCollection.Builder) {
    }

    /**
     * Registers custom serializers with the provided builder.
     */
    @Suppress("UNCHECKED_CAST")
    fun buildSerializersModule(): Consumer<TypeSerializerCollection.Builder> = Consumer { builder ->
        classSerializers.forEach { (clazz, serializer) ->
            builder.register(clazz as Class<Any>, serializer as TypeSerializer<Any>)
        }

        typeTokenSerializers.forEach { (type, serializer) ->
            builder.register(type as TypeToken<Any>, serializer as TypeSerializer<Any>)
        }

        builder.register(ComponentSerializer)
        builder.register(TextColorSerializer)
        builder.register(LinkedListSerializer.TYPE, LinkedListSerializer)
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
}
