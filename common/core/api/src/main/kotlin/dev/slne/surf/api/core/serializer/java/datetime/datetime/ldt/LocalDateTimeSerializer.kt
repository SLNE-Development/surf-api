@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.core.serializer.java.datetime.datetime.ldt

import dev.slne.surf.api.core.serializer.java.datetime.date.local.LocalDateSerializer
import dev.slne.surf.api.core.serializer.java.datetime.time.local.LocalTimeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

typealias SerializableLocalDateTime = @Serializable(with = LocalDateTimeSerializer::class) LocalDateTime

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor =
        buildClassSerialDescriptor("surf.api.java.datetime.datetime.LocalDateTime") {
            element("date", LocalDateSerializer.descriptor)
            element("time", LocalTimeSerializer.descriptor)
        }

    override fun serialize(
        encoder: Encoder,
        value: LocalDateTime,
    ) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, LocalDateSerializer, value.toLocalDate())
        encodeSerializableElement(descriptor, 1, LocalTimeSerializer, value.toLocalTime())
    }

    override fun deserialize(
        decoder: Decoder,
    ): LocalDateTime = decoder.decodeStructure(descriptor) {
        var date: LocalDate? = null
        var time: LocalTime? = null

        if (decodeSequentially()) {
            date = decodeSerializableElement(descriptor, 0, LocalDateSerializer)
            time = decodeSerializableElement(descriptor, 1, LocalTimeSerializer)
        } else while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> date = decodeSerializableElement(descriptor, 0, LocalDateSerializer)
                1 -> time = decodeSerializableElement(descriptor, 1, LocalTimeSerializer)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        require(date != null) { "Missing value for date" }
        require(time != null) { "Missing value for time" }

        LocalDateTime.of(date, time)
    }
}