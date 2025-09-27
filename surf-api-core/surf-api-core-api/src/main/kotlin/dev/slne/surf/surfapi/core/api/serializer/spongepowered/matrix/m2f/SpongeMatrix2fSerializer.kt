package dev.slne.surf.surfapi.core.api.serializer.spongepowered.matrix.m2f

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.matrix.Matrix2f

typealias SerializableMatrix2f = @Serializable(with = SpongeMatrix2fSerializer::class) Matrix2f

object SpongeMatrix2fSerializer : KSerializer<Matrix2f> {
    private val arraySerializer = FloatArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Matrix2f", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Matrix2f,
    ) {
        encoder.encodeSerializableValue(arraySerializer, value.toArray())
    }

    override fun deserialize(decoder: Decoder): Matrix2f {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 4) { "Expected array of size 4, got ${array.size}" }
        return Matrix2f(
            array[0], array[1],
            array[2], array[3],
        )
    }
}