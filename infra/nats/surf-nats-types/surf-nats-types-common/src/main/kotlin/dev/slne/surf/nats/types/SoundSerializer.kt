package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import java.util.OptionalLong

object SoundSerializer : KSerializer<Sound> {
    override val descriptor: SerialDescriptor = SoundProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Sound) {
        val seed = value.seed()
        encoder.encodeSerializableValue(
            SoundProto.serializer(),
            SoundProto(
                value.name(),
                sourceNumber(value.source()),
                value.volume(),
                value.pitch(),
                if (seed.isPresent) seed.asLong else null,
            )
        )
    }

    override fun deserialize(decoder: Decoder): Sound {
        val proto = decoder.decodeSerializableValue(SoundProto.serializer())

        return Sound.sound()
            .type(proto.name)
            .source(source(proto.source))
            .volume(proto.volume)
            .pitch(proto.pitch)
            .seed(proto.seed?.let { OptionalLong.of(it) } ?: OptionalLong.empty())
            .build()
    }

    private fun sourceNumber(source: Sound.Source): Int = when (source) {
        Sound.Source.MASTER -> 1
        Sound.Source.MUSIC -> 2
        Sound.Source.RECORD -> 3
        Sound.Source.WEATHER -> 4
        Sound.Source.BLOCK -> 5
        Sound.Source.HOSTILE -> 6
        Sound.Source.NEUTRAL -> 7
        Sound.Source.PLAYER -> 8
        Sound.Source.AMBIENT -> 9
        Sound.Source.VOICE -> 10
        Sound.Source.UI -> 11
    }

    private fun source(number: Int): Sound.Source = when (number) {
        2 -> Sound.Source.MUSIC
        3 -> Sound.Source.RECORD
        4 -> Sound.Source.WEATHER
        5 -> Sound.Source.BLOCK
        6 -> Sound.Source.HOSTILE
        7 -> Sound.Source.NEUTRAL
        8 -> Sound.Source.PLAYER
        9 -> Sound.Source.AMBIENT
        10 -> Sound.Source.VOICE
        11 -> Sound.Source.UI
        else -> Sound.Source.MASTER
    }
}

@Serializable
@SerialName("surf.types.v1.Sound")
private class SoundProto(
    @ProtoNumber(1) val name: @Serializable(with = KeySerializer::class) Key,
    @ProtoNumber(2) val source: Int = 0,
    @ProtoNumber(3) val volume: Float,
    @ProtoNumber(4) val pitch: Float,
    @ProtoNumber(5) val seed: Long? = null,
)
