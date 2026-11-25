package dev.slne.surf.redis.codec

import dev.slne.surf.api.serialization.SurfSerializerModule
import dev.slne.surf.api.serialization.adventure.resourcepack.info.AdventureResourcePackInfoSerializer
import dev.slne.surf.api.serialization.java.url.URLSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import net.kyori.adventure.resource.ResourcePackInfo
import java.net.URI
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonKotlinCodecUuidTest {

    private val codec = JsonKotlinCodec.of(AdventureResourcePackInfoSerializer)
    private val info = ResourcePackInfo.resourcePackInfo(
        UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"),
        URI.create("https://example.invalid/pack.zip"),
        "hash",
    )

    @Test
    fun `nested uuid is written as a string`() {
        val json = codec.json.encodeToString(AdventureResourcePackInfoSerializer, info)

        assertEquals(
            """{"id":"00112233-4455-6677-8899-aabbccddeeff",""" +
                """"uri":"https://example.invalid/pack.zip","hash":"hash"}""",
            json,
        )
        assertEquals(info, codec.json.decodeFromString(AdventureResourcePackInfoSerializer, json))
    }

    @Test
    fun `nested uuid falls back to bytes without a contextual serializer`() {
        val json = Json.encodeToString(AdventureResourcePackInfoSerializer, info)

        assertEquals(
            """{"id":[0,17,34,51,68,85,102,119,-120,-103,-86,-69,-52,-35,-18,-1],""" +
                """"uri":"https://example.invalid/pack.zip","hash":"hash"}""",
            json,
        )
        assertEquals(info, Json.decodeFromString(AdventureResourcePackInfoSerializer, json))
    }

    @Test
    fun `url stays a valid map key`() {
        val serializer = MapSerializer(URLSerializer, Int.serializer())
        val value = mapOf(URI.create("https://example.invalid/").toURL() to 1)
        val formats = listOf(Json, Json { serializersModule = SurfSerializerModule.all })

        for (format in formats) {
            val json = format.encodeToString(serializer, value)

            assertEquals("""{"https://example.invalid/":1}""", json)
            val decoded = format.decodeFromString(serializer, json)
            assertEquals(json, format.encodeToString(serializer, decoded))
        }
    }
}
