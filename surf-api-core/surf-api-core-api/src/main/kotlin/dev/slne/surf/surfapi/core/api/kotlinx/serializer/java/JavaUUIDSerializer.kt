package dev.slne.surf.surfapi.core.api.kotlinx.serializer.java

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.ByteBuffer
import java.util.*

typealias SerializableUUID = @Serializable(with = JavaUUIDSerializer::class) UUID

object JavaUUIDSerializer : KSerializer<UUID> {
    private val byteArraySerializer = ByteArraySerializer()
    override val descriptor = byteArraySerializer.descriptor

    override fun serialize(encoder: Encoder, value: UUID) {
        val bytes = ByteArray(16)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.putLong(value.mostSignificantBits)
        buffer.putLong(value.leastSignificantBits)

        encoder.encodeSerializableValue(byteArraySerializer, bytes)
    }

    override fun deserialize(decoder: Decoder): UUID {
        val bytes = decoder.decodeSerializableValue(byteArraySerializer)
        val buffer = ByteBuffer.wrap(bytes)
        val mostSigBits = buffer.long
        val leastSigBits = buffer.long

        return UUID(mostSigBits, leastSigBits)
    }
}