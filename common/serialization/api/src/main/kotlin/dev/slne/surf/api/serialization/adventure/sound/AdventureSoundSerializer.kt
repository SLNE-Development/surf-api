@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.serialization.adventure.sound

import dev.slne.surf.api.serialization.adventure.key.AdventureKeySerializer
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound

typealias SerializableSound = @Serializable(with = AdventureSoundSerializer::class) Sound

object AdventureSoundSerializer : KSerializer<Sound> {
    private val keySerializer = ContextualSerializer(
        Key::class,
        AdventureKeySerializer,
        emptyArray(),
    )

    override val descriptor = buildClassSerialDescriptor("surf.api.Sound") {
        element("name", keySerializer.descriptor)
        element("source", serializer<Sound.Source>().descriptor)
        element<Float>("volume")
        element<Float>("pitch")
        element<Long>("seed", isOptional = true)
    }

    override fun serialize(
        encoder: Encoder,
        value: Sound,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, keySerializer, value.name())
            encodeSerializableElement(descriptor, 1, serializer<Sound.Source>(), value.source())
            encodeFloatElement(descriptor, 2, value.volume())
            encodeFloatElement(descriptor, 3, value.pitch())
            value.seed().ifPresent { seed ->
                encodeLongElement(descriptor, 4, seed)
            }
        }
    }

    override fun deserialize(decoder: Decoder): Sound {
        return decoder.decodeStructure(descriptor) {
            var name: Key? = null
            var source: Sound.Source? = null
            var volume: Float? = null
            var pitch: Float? = null
            var seed: Long? = null

            if (decodeSequentially()) {
                name = decodeSerializableElement(descriptor, 0, keySerializer)
                source = decodeSerializableElement(descriptor, 1, serializer<Sound.Source>())
                volume = decodeFloatElement(descriptor, 2)
                pitch = decodeFloatElement(descriptor, 3)
                seed = decodeNullableSerializableElement(descriptor, 4, Long.serializer())
            } else while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> name = decodeSerializableElement(descriptor, 0, keySerializer)
                    1 -> source = decodeSerializableElement(
                        descriptor,
                        1,
                        serializer<Sound.Source>(),
                    )

                    2 -> volume = decodeFloatElement(descriptor, 2)
                    3 -> pitch = decodeFloatElement(descriptor, 3)
                    4 -> seed = decodeNullableSerializableElement(descriptor, 4, Long.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            Sound.sound()
                .type(name ?: error("Missing name"))
                .source(source ?: error("Missing source"))
                .volume(volume ?: error("Missing volume"))
                .pitch(pitch ?: error("Missing pitch"))
                .apply { seed?.let { seed(it) } }
                .build()
        }
    }
}
