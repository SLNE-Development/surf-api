package dev.slne.surf.surfapi.core.api.serializer.spongepowered.matrix.m2d

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.matrix.Matrix2d

typealias SerializableMatrix2d = @Serializable(with = SpongeMatrix2dSerializer::class) Matrix2d

object SpongeMatrix2dSerializer : KSerializer<Matrix2d> {
    private val arraySerializer = DoubleArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Matrix2d", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Matrix2d,
    ) {
        encoder.encodeSerializableValue(arraySerializer, value.toArray())
    }

    override fun deserialize(decoder: Decoder): Matrix2d {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 4) { "Expected array of size 4, got ${array.size}" }
        return Matrix2d(
            array[0], array[1],
            array[2], array[3],
        )
    }
}