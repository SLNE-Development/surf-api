package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import java.time.DateTimeException
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor = ZonedDateTimeProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeSerializableValue(
            ZonedDateTimeProto.serializer(),
            ZonedDateTimeProto(value.toOffsetDateTime(), value.zone.id)
        )
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        val proto = decoder.decodeSerializableValue(ZonedDateTimeProto.serializer())
        val zone = try {
            ZoneId.of(proto.zoneId)
        } catch (e: DateTimeException) {
            throw SerializationException("${descriptor.serialName}.zone_id '${proto.zoneId}' is not a known zone", e)
        }

        return try {
            ZonedDateTime.ofInstant(proto.dateTime.toLocalDateTime(), proto.dateTime.offset, zone)
        } catch (e: DateTimeException) {
            throw SerializationException(
                "${descriptor.serialName}.date_time is outside the java.time.ZonedDateTime range in ${proto.zoneId}",
                e
            )
        }
    }
}

@Serializable
@SerialName("surf.types.v1.ZonedDateTime")
private class ZonedDateTimeProto(
    @ProtoNumber(1) val dateTime: @Serializable(with = OffsetDateTimeSerializer::class) OffsetDateTime,
    @ProtoNumber(2) val zoneId: String,
)
