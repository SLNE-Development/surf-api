@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.offset

import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.ldt.LocalDateTimeSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.offset.ZoneOffsetSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

typealias SerializableOffsetDateTime = @Serializable(with = OffsetDateTimeSerializer::class) OffsetDateTime

object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor =
        buildClassSerialDescriptor("surfapi.java.datetime.datetime.OffsetDateTime") {
            element("offset", ZoneOffsetSerializer.descriptor)
            element("localDateTime", LocalDateTimeSerializer.descriptor)
        }

    override fun serialize(
        encoder: Encoder,
        value: OffsetDateTime,
    ) {
        encoder.encodeString(DateTimeFormatter.ISO_DATE_TIME.format(value))
    }

    override fun deserialize(
        decoder: Decoder,
    ): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE_TIME)
    }
}