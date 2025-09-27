@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.adventure.sound.stop

import dev.slne.surf.surfapi.core.api.serializer.adventure.key.AdventureKeySerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.AdventureSoundSerializer.SoundSourceSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop

typealias SerializableSoundStop = @Serializable(with = AdventureSoundStopSerializer::class) SoundStop

object AdventureSoundStopSerializer : KSerializer<SoundStop> {
    override val descriptor = buildClassSerialDescriptor("surfapi.SoundStop") {
        element("source", SoundSourceSerializer.descriptor, isOptional = true)
        element("sound", AdventureKeySerializer.descriptor, isOptional = true)
    }

    override fun serialize(
        encoder: Encoder,
        value: SoundStop,
    ) = encoder.encodeStructure(descriptor) {
        encodeNullableSerializableElement(
            descriptor,
            0,
            SoundSourceSerializer,
            value.source()
        )

        encodeNullableSerializableElement(
            descriptor,
            1,
            AdventureKeySerializer,
            value.sound()
        )
    }

    override fun deserialize(decoder: Decoder): SoundStop = decoder.decodeStructure(descriptor) {
        var source: Sound.Source? = null
        var sound: Key? = null

        if (decodeSequentially()) {
            source = decodeNullableSerializableElement(descriptor, 0, SoundSourceSerializer)
            sound = decodeNullableSerializableElement(descriptor, 1, AdventureKeySerializer)
        } else while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> source =
                    decodeNullableSerializableElement(descriptor, 0, SoundSourceSerializer)

                1 -> sound =
                    decodeNullableSerializableElement(descriptor, 1, AdventureKeySerializer)

                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        when {
            source == null && sound == null -> SoundStop.all()
            source != null && sound != null -> SoundStop.namedOnSource(sound, source)
            source != null -> SoundStop.source(source)
            sound != null -> SoundStop.named(sound)
            else -> throw MatchException("Unreachable", null)
        }
    }
}