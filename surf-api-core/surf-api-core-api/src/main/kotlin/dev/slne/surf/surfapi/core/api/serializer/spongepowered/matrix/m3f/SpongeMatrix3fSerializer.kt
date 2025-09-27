package dev.slne.surf.surfapi.core.api.serializer.spongepowered.matrix.m3f

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.matrix.Matrix3f

typealias SerializableMatrix3f = @Serializable(with = SpongeMatrix3fSerializer::class) Matrix3f

object SpongeMatrix3fSerializer : KSerializer<Matrix3f> {
    private val arraySerializer = FloatArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Matrix3f", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Matrix3f,
    ) {
        encoder.encodeSerializableValue(arraySerializer, value.toArray())
    }

    override fun deserialize(decoder: Decoder): Matrix3f {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 9) { "Expected array of size 9, got ${array.size}" }
        return Matrix3f(
            array[0], array[1], array[2],
            array[3], array[4], array[5],
            array[6], array[7], array[8],
        )
    }
}