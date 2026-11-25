package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

/**
 * Serializes a value as a protobuf message whose only field, number 1, holds the value as written
 * by [inner].
 *
 * The wrapper lets a surf-api serializer, which writes a bare string, number or byte array, stand
 * for a single-field message such as `surf.types.v1.Uuid`. [inner] must write exactly one
 * primitive, string or byte array, never a structure. Encoding always writes the field, also for a
 * default value such as `0`.
 *
 * @param T the type of the wrapped value
 * @param serialName the fully qualified protobuf message name, for example `surf.types.v1.Uuid`
 * @param inner the serializer that writes the value of the field
 * @param fieldName the field name declared in the `.proto` file
 * @param integerType the protobuf encoding of an integer field
 * @throws MissingFieldException on decoding when the field is absent
 * @throws SerializationException on decoding when [inner] rejects the value
 */
open class ProtoWrapperSerializer<T : Any>(
    serialName: String,
    private val inner: KSerializer<T>,
    private val fieldName: String = "value",
    integerType: ProtoIntegerType = ProtoIntegerType.DEFAULT,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName) {
        element(
            fieldName,
            inner.descriptor,
            annotations = fieldAnnotations(integerType),
            isOptional = true,
        )
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, inner, value)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        return decoder.decodeStructure(descriptor) {
            var value: T? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> value = decodeField(this)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException(
                        "${descriptor.serialName} has no field with index $index",
                    )
                }
            }

            value ?: throw MissingFieldException(fieldName, descriptor.serialName)
        }
    }

    private fun decodeField(decoder: CompositeDecoder): T {
        return try {
            decoder.decodeSerializableElement(descriptor, 0, inner)
        } catch (e: SerializationException) {
            throw e
        } catch (e: RuntimeException) {
            throw SerializationException("${descriptor.serialName}.$fieldName is invalid", e)
        }
    }

    private fun fieldAnnotations(integerType: ProtoIntegerType): List<Annotation> {
        return if (integerType == ProtoIntegerType.DEFAULT) {
            listOf(ProtoNumber(1))
        } else {
            listOf(ProtoNumber(1), ProtoType(integerType))
        }
    }
}
