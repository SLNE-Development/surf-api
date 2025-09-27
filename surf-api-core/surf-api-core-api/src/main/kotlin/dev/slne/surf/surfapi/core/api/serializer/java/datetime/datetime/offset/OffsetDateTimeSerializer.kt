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
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

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
    ) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(
            descriptor,
            0,
            ZoneOffsetSerializer,
            value.offset
        )
        encodeSerializableElement(
            descriptor,
            1,
            LocalDateTimeSerializer,
            value.toLocalDateTime()
        )
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var offset: ZoneOffset? = null
        var localDateTime: LocalDateTime? = null

        if (decodeSequentially()) {
            offset = decodeSerializableElement(
                descriptor,
                0,
                ZoneOffsetSerializer
            )
            localDateTime = decodeSerializableElement(
                descriptor,
                1,
                LocalDateTimeSerializer
            )
        } else while (true) {
            when (decodeElementIndex(descriptor)) {
                0 -> offset = decodeSerializableElement(
                    descriptor,
                    0,
                    ZoneOffsetSerializer
                )

                1 -> localDateTime = decodeSerializableElement(
                    descriptor,
                    1,
                    LocalDateTimeSerializer
                )

                else -> break
            }
        }

        require(offset != null) { "Missing value for offset" }
        require(localDateTime != null) { "Missing value for localDateTime" }

        OffsetDateTime.of(localDateTime, offset)
    }
}