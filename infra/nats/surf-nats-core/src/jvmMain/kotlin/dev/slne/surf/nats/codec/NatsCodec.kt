package dev.slne.surf.nats.codec

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolHeaders
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Encodes and decodes message payloads as protobuf, the only wire format.
 *
 * Default values are not encoded.
 */
@OptIn(ExperimentalSerializationApi::class)
@InternalNatsApi
internal object NatsCodec {

    private val protoBuf = ProtoBuf {
        encodeDefaults = false
    }

    fun <T> encode(serializer: SerializationStrategy<T>, value: T): ByteArray {
        return protoBuf.encodeToByteArray(serializer, value)
    }

    /**
     * Decodes [bytes] with [deserializer].
     *
     * Throws [IllegalArgumentException] when [contentType] is `null` or its media type is not `application/protobuf`,
     * ignoring case and parameters. Throws [SerializationException] when the payload is malformed.
     */
    fun <T> decode(deserializer: DeserializationStrategy<T>, bytes: ByteArray, contentType: String?): T {
        ProtocolHeaders.requireProtobufContentType(contentType)
        return try {
            protoBuf.decodeFromByteArray(deserializer, bytes)
        } catch (e: SerializationException) {
            throw e
        } catch (e: RuntimeException) {
            // kotlinx-protobuf reports truncated input as IndexOutOfBoundsException
            throw SerializationException(
                "Malformed protobuf payload for ${deserializer.descriptor.serialName}: ${e.message}",
                e
            )
        }
    }
}
