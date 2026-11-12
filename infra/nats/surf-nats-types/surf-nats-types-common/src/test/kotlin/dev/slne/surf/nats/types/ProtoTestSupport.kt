package dev.slne.surf.nats.types

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Same configuration as the runtime codec.
internal val proto = ProtoBuf { encodeDefaults = false }

internal fun <T> encodeHex(serializer: SerializationStrategy<T>, value: T): String =
    proto.encodeToByteArray(serializer, value).toHexString()

internal fun <T> decodeHex(deserializer: DeserializationStrategy<T>, hex: String): T =
    proto.decodeFromByteArray(deserializer, hex.hexToByteArray())

internal fun assertMessageDescriptor(
    descriptor: SerialDescriptor,
    serialName: String,
    vararg fields: Pair<String, Int>,
) {
    assertEquals(serialName, descriptor.serialName)
    assertEquals(StructureKind.CLASS, descriptor.kind)
    assertEquals(fields.map { it.first }, descriptor.elementNames.toList())
    fields.forEachIndexed { index, (_, number) ->
        val protoNumber = descriptor.getElementAnnotations(index).filterIsInstance<ProtoNumber>().single()
        assertEquals(number, protoNumber.number)
    }
}

internal inline fun assertMissingField(serialName: String, vararg fields: String, block: () -> Unit) {
    val exception = assertFailsWith<SerializationException>(block = block)
    val missing = generateSequence<Throwable>(exception) { it.cause }
        .filterIsInstance<MissingFieldException>()
        .firstOrNull()

    assertNotNull(missing, "Expected a MissingFieldException, got $exception")
    assertEquals(serialName, missing.serialName)
    fields.forEach { assertTrue(it in missing.missingFields, "$it not in ${missing.missingFields}") }
}

internal inline fun assertInvalid(block: () -> Unit) {
    val exception = assertFailsWith<SerializationException>(block = block)
    val missing = generateSequence<Throwable>(exception) { it.cause }.any { it is MissingFieldException }

    assertTrue(!missing, "Expected an invalid value, not a missing field: $exception")
}
