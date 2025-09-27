@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.zdt

import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.instant.InstantSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.id.ZonedIdSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

typealias SerializableZonedDateTime = @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor =
        buildClassSerialDescriptor("surfapi.java.datetime.datetime.ZonedDateTime") {
            element("zoneId", ZonedIdSerializer.descriptor)
            element("instant", InstantSerializer.descriptor)
        }

    override fun serialize(
        encoder: Encoder,
        value: ZonedDateTime,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ZonedIdSerializer, value.zone)
            encodeSerializableElement(descriptor, 1, InstantSerializer, value.toInstant())
        }
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime =
        decoder.decodeStructure(descriptor) {
            var zoneId: ZoneId? = null
            var instant: Instant? = null

            if (decodeSequentially()) {
                zoneId = decodeSerializableElement(descriptor, 0, ZonedIdSerializer)
                instant = decodeSerializableElement(descriptor, 1, InstantSerializer)
            } else while (true) {
                when (decodeElementIndex(descriptor)) {
                    0 -> zoneId = decodeSerializableElement(descriptor, 0, ZonedIdSerializer)
                    1 -> instant = decodeSerializableElement(descriptor, 1, InstantSerializer)
                    else -> break
                }
            }

            require(zoneId != null) { "Missing value for zoneId" }
            require(instant != null) { "Missing value for instant" }

            ZonedDateTime.ofInstant(instant, zoneId)
        }
}