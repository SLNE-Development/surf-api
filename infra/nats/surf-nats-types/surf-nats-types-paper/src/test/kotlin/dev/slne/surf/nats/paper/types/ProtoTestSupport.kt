package dev.slne.surf.nats.paper.types

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
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

internal fun lengthDelimited(fieldNumber: Int, bytes: ByteArray): String =
    varint((fieldNumber shl 3 or 2).toLong()) + varint(bytes.size.toLong()) + bytes.toHexString()

private fun varint(value: Long): String = buildString {
    var remaining = value
    while (remaining and 0x7FL.inv() != 0L) {
        append(((remaining and 0x7F) or 0x80).toInt().toByte().toHexString())
        remaining = remaining ushr 7
    }
    append(remaining.toInt().toByte().toHexString())
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
