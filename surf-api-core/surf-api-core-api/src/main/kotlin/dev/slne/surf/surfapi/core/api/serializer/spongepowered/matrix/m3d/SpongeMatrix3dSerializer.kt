package dev.slne.surf.surfapi.core.api.serializer.spongepowered.matrix.m3d

import dev.slne.surf.surfapi.core.api.serializer.spongepowered.matrix.m2d.SpongeMatrix2dSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.matrix.Matrix3d

typealias SerializableMatrix3d = @Serializable(with = SpongeMatrix2dSerializer::class) Matrix3d

object SpongeMatrix3dSerializer : KSerializer<Matrix3d> {
    private val arraySerializer = DoubleArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Matrix3d", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Matrix3d,
    ) {
        encoder.encodeSerializableValue(arraySerializer, value.toArray())
    }

    override fun deserialize(decoder: Decoder): Matrix3d {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 9) { "Expected array of size 9, got ${array.size}" }
        return Matrix3d(
            array[0], array[1], array[2],
            array[3], array[4], array[5],
            array[6], array[7], array[8],
        )
    }
}