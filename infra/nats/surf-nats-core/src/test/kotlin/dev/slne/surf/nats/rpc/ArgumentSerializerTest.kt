package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.codec.NatsCodec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArgumentSerializerTest {

    private val codec = NatsCodec(EmptySerializersModule())

    @Suppress("UNCHECKED_CAST")
    private val arguments = ArgumentSerializer(
        serialName = "player.health",
        names = listOf("playerName", "level", "note"),
        serializer = listOf(
            String.serializer(),
            Int.serializer(),
            String.serializer().nullable,
        ) as List<KSerializer<Any?>>,
    )

    private fun decodeJson(json: String): Array<Any?> {
        return codec.decode(arguments, json.encodeToByteArray(), WireFormat.JSON.contentType)
    }

    @Test
    fun `roundtrip in both formats`() {
        val values = arrayOf<Any?>("Twisti", 7, null)
        for (format in WireFormat.entries) {
            val bytes = codec.encode(arguments, values, format)
            assertContentEquals(values, codec.decode(arguments, bytes, format.contentType), "format $format")
        }
    }

    @Test
    fun `json uses snake case parameter names`() {
        val json = codec.encode(arguments, arrayOf("Twisti", 7, "hi"), WireFormat.JSON).decodeToString()
        assertEquals("""{"player_name":"Twisti","level":7,"note":"hi"}""", json)
    }

    @Test
    fun `missing nullable argument becomes null`() {
        assertContentEquals(
            arrayOf("Twisti", 7, null),
            decodeJson("""{"player_name":"Twisti","level":7}"""),
        )
    }

    @Test
    fun `missing non-null argument is rejected`() {
        assertFailsWith<SerializationException> { decodeJson("""{"player_name":"Twisti"}""") }
    }

    @Test
    fun `unknown arguments are ignored`() {
        assertContentEquals(
            arrayOf("Twisti", 7, null),
            decodeJson("""{"player_name":"Twisti","level":7,"removed":true}"""),
        )
    }
}
