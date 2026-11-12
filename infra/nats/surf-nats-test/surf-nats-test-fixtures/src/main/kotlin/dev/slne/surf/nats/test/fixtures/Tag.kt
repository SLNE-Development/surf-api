package dev.slne.surf.nats.test.fixtures

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A String-backed value class that the fixtures map to the proto message `surf.nats.test.v1.Tag`.
 */
@JvmInline
value class Tag(val value: String)

object TagSerializer : KSerializer<Tag> {
    override val descriptor: SerialDescriptor = TagProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Tag) {
        encoder.encodeSerializableValue(TagProto.serializer(), TagProto(value.value))
    }

    override fun deserialize(decoder: Decoder): Tag = Tag(decoder.decodeSerializableValue(TagProto.serializer()).value)
}

@Serializable
@SerialName("surf.nats.test.v1.Tag")
private class TagProto(
    @ProtoNumber(1) val value: String,
)
