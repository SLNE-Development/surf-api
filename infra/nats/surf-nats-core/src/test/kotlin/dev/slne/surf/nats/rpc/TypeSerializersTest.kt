package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.codec.NatsCodec
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import kotlin.reflect.KType
import kotlin.reflect.full.declaredFunctions
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeSerializersTest {

    @Suppress("unused")
    interface Types {
        suspend fun plain(): String
        suspend fun nullable(): String?
        suspend fun custom(): @Serializable(with = UpperCaseSerializer::class) String
        suspend fun customNullable(): @Serializable(with = UpperCaseSerializer::class) String?
        suspend fun customClass(): @Serializable(with = ReversedSerializer::class) String
        suspend fun nested(): List<@Serializable(with = UpperCaseSerializer::class) String>
    }

    private val codec = NatsCodec(EmptySerializersModule())
    private val module = codec.serializersModule(WireFormat.JSON)

    private fun returnType(name: String): KType =
        Types::class.declaredFunctions.single { it.name == name }.returnType

    private fun json(name: String, value: Any?): String {
        val serializer = TypeSerializers.serializerFor(returnType(name), module)
        return codec.encode(serializer, value, WireFormat.JSON).decodeToString()
    }

    @Test
    fun `plain type uses the default serializer`() {
        assertEquals("\"abc\"", json("plain", "abc"))
    }

    @Test
    fun `nullable type accepts null`() {
        assertEquals("null", json("nullable", null))
    }

    @Test
    fun `object serializer from type annotation is used`() {
        assertEquals("\"ABC\"", json("custom", "abc"))
    }

    @Test
    fun `custom serializer is wrapped for nullable types`() {
        assertEquals("null", json("customNullable", null))
        assertEquals("\"ABC\"", json("customNullable", "abc"))
    }

    @Test
    fun `class serializer is instantiated through its constructor`() {
        assertEquals("\"cba\"", json("customClass", "abc"))
    }

    @Test
    fun `annotation on a type argument is not applied`() {
        assertEquals("[\"abc\"]", json("nested", listOf("abc")))
    }
}
