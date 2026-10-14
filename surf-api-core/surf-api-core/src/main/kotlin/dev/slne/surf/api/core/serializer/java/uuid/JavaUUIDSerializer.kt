package dev.slne.surf.api.core.serializer.java.uuid

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.invoke.MethodHandles
import java.nio.ByteOrder
import java.util.*

typealias SerializableUUID = @Serializable(with = JavaUUIDSerializer::class) UUID

object JavaUUIDSerializer : KSerializer<UUID> {
    private val byteArraySerializer = ByteArraySerializer()
    private val longBigEndian = MethodHandles.byteArrayViewVarHandle(
        LongArray::class.java,
        ByteOrder.BIG_ENDIAN,
    )

    override val descriptor = SerialDescriptor("surf.api.UUID", byteArraySerializer.descriptor)

    override fun serialize(encoder: Encoder, value: UUID) {
        val bytes = ByteArray(16)
        longBigEndian.set(bytes, 0, value.mostSignificantBits)
        longBigEndian.set(bytes, 8, value.leastSignificantBits)

        encoder.encodeSerializableValue(byteArraySerializer, bytes)
    }

    override fun deserialize(decoder: Decoder): UUID {
        val bytes = decoder.decodeSerializableValue(byteArraySerializer)
        require(bytes.size == 16) { "UUID must be 16 bytes, but was ${bytes.size}" }

        val mostSigBits = longBigEndian.get(bytes, 0) as Long
        val leastSigBits = longBigEndian.get(bytes, 8) as Long

        return UUID(mostSigBits, leastSigBits)
    }
}
