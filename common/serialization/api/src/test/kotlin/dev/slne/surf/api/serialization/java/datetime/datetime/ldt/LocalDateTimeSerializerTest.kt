package dev.slne.surf.api.serialization.java.datetime.datetime.ldt

import dev.slne.surf.api.serialization.java.datetime.date.local.LocalDateSerializer
import dev.slne.surf.api.serialization.java.datetime.time.local.LocalTimeSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateTimeSerializerTest {

    private val value = LocalDateTime.of(2026, 11, 21, 13, 37, 42)

    @Test
    fun `nested date and time use the fallback serializers without a contextual one`() {
        val date = Json.encodeToString(LocalDateSerializer, value.toLocalDate())
        val time = Json.encodeToString(LocalTimeSerializer, value.toLocalTime())

        val json = Json.encodeToString(LocalDateTimeSerializer, value)

        assertEquals("""{"date":$date,"time":$time}""", json)
        assertEquals(value, Json.decodeFromString(LocalDateTimeSerializer, json))
    }

    @Test
    fun `a contextual date serializer replaces the nested date`() {
        val json = Json {
            serializersModule = SerializersModule {
                contextual(LocalDate::class, IsoLocalDateSerializer)
            }
        }
        val time = Json.encodeToString(LocalTimeSerializer, value.toLocalTime())

        val encoded = json.encodeToString(LocalDateTimeSerializer, value)

        assertEquals("""{"date":"2026-11-21","time":$time}""", encoded)
        assertEquals(value, json.decodeFromString(LocalDateTimeSerializer, encoded))
    }

    private object IsoLocalDateSerializer : KSerializer<LocalDate> {
        override val descriptor = PrimitiveSerialDescriptor(
            "test.IsoLocalDate",
            PrimitiveKind.STRING,
        )

        override fun serialize(encoder: Encoder, value: LocalDate) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): LocalDate {
            return LocalDate.parse(decoder.decodeString())
        }
    }
}
