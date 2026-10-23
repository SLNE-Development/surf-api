package dev.slne.surf.nats.codec

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolHeaders
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
@InternalNatsApi
internal object NatsCodec {

    private val protoBuf = ProtoBuf {
        encodeDefaults = false
    }

    fun <T> encode(serializer: SerializationStrategy<T>, value: T): ByteArray {
        return protoBuf.encodeToByteArray(serializer, value)
    }

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
