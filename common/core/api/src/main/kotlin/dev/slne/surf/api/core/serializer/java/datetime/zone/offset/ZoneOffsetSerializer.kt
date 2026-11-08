package dev.slne.surf.api.core.serializer.java.datetime.zone.offset

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneOffset

typealias SerializableZoneOffset = @Serializable(with = ZoneOffsetSerializer::class) ZoneOffset

object ZoneOffsetSerializer : KSerializer<ZoneOffset> {
    override val descriptor = PrimitiveSerialDescriptor(
        "surf.api.java.datetime.zone.ZoneOffset",
        PrimitiveKind.STRING
    )

    override fun serialize(
        encoder: Encoder,
        value: ZoneOffset,
    ) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): ZoneOffset =
        ZoneOffset.of(decoder.decodeString())
}