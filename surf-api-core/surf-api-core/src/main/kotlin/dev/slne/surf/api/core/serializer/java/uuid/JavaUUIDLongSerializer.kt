package dev.slne.surf.api.core.serializer.java.uuid

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import java.util.*

typealias SerializableLongUUID = @Serializable(with = JavaUUIDLongSerializer::class) UUID

object JavaUUIDLongSerializer : KSerializer<UUID> {
    override val descriptor = buildClassSerialDescriptor("surf.api.LongUuid") {
        element<Long>("most")
        element<Long>("least")
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.mostSignificantBits)
            encodeLongElement(descriptor, 1, value.leastSignificantBits)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): UUID = decoder.decodeStructure(descriptor) {
        if (decodeSequentially()) {
            return@decodeStructure UUID(
                decodeLongElement(descriptor, 0),
                decodeLongElement(descriptor, 1),
            )
        }

        var most = 0L
        var least = 0L
        var seen = 0
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> {
                    most = decodeLongElement(descriptor, 0)
                    seen = seen or 1
                }

                1 -> {
                    least = decodeLongElement(descriptor, 1)
                    seen = seen or 2
                }

                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected element index $index")
            }
        }

        if (seen != 3) throw SerializationException("UUID requires both 'most' and 'least'")

        UUID(most, least)
    }
}
