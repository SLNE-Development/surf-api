package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

object TitleSerializer : KSerializer<Title> {
    override val descriptor: SerialDescriptor = TitleProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Title) {
        val times = value.times()?.let {
            TimesProto(it.fadeIn().toKotlinDuration(), it.stay().toKotlinDuration(), it.fadeOut().toKotlinDuration())
        }

        encoder.encodeSerializableValue(TitleProto.serializer(), TitleProto(value.title(), value.subtitle(), times))
    }

    override fun deserialize(decoder: Decoder): Title {
        val proto = decoder.decodeSerializableValue(TitleProto.serializer())
        val times = proto.times?.let {
            Title.Times.times(it.fadeIn.toJavaDuration(), it.stay.toJavaDuration(), it.fadeOut.toJavaDuration())
        }

        return Title.title(proto.title, proto.subtitle, times)
    }
}

@Serializable
@SerialName("surf.types.v1.Title")
private class TitleProto(
    @ProtoNumber(1) val title: @Serializable(with = ComponentSerializer::class) Component,
    @ProtoNumber(2) val subtitle: @Serializable(with = ComponentSerializer::class) Component,
    @ProtoNumber(3) val times: TimesProto? = null,
)

@Serializable
@SerialName("surf.types.v1.Title.Times")
private class TimesProto(
    @ProtoNumber(1) val fadeIn: @Serializable(with = DurationSerializer::class) Duration,
    @ProtoNumber(2) val stay: @Serializable(with = DurationSerializer::class) Duration,
    @ProtoNumber(3) val fadeOut: @Serializable(with = DurationSerializer::class) Duration,
)
