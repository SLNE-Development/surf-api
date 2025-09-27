package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m4d

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.matrix.Matrix4d

typealias SerializableMatrix4d = @Serializable(with = SpongeMatrix4dSerializer::class) Matrix4d

object SpongeMatrix4dSerializer : KSerializer<Matrix4d> {
    private val arraySerializer = DoubleArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Matrix4d", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Matrix4d,
    ) {
        encoder.encodeSerializableValue(arraySerializer, value.toArray())
    }

    override fun deserialize(decoder: Decoder): Matrix4d {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 16) { "Expected array of size 16, got ${array.size}" }
        return Matrix4d(
            array[0], array[1], array[2], array[3],
            array[4], array[5], array[6], array[7],
            array[8], array[9], array[10], array[11],
            array[12], array[13], array[14], array[15],
        )
    }
}