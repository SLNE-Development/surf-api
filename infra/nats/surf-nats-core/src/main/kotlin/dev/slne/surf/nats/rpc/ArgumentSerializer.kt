package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

@InternalNatsApi
internal class ArgumentSerializer(
    private val serialName: String, // service.method-kebab
    private val names: List<String>,
    private val serializer: List<KSerializer<Any?>>
) : KSerializer<Array<Any?>> {

    override val descriptor = buildClassSerialDescriptor(serialName) {
        names.forEachIndexed { i, name ->
            element(
                name,
                serializer[i].descriptor,
                isOptional = serializer[i].descriptor.isNullable
            )
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Array<Any?>
    ) = encoder.encodeStructure(descriptor) {
        value.forEachIndexed { i, arg ->
            encodeSerializableElement(descriptor, i, serializer[i], arg)
        }
    }

    override fun deserialize(decoder: Decoder): Array<Any?> = decoder.decodeStructure(descriptor) {
        val result = arrayOfNulls<Any?>(names.size)
        val present = BooleanArray(names.size)
        while (true) {
            val i = decodeElementIndex(descriptor)
            if (i == CompositeDecoder.DECODE_DONE) break
            result[i] = decodeSerializableElement(descriptor, i, serializer[i])
            present[i] = true
        }

        for (i in names.indices) {
            if (!present[i] && !descriptor.isElementOptional(i)) {
                throw SerializationException("Missing argument '${names[i]}' for $serialName")
            }
        }

        result
    }
}