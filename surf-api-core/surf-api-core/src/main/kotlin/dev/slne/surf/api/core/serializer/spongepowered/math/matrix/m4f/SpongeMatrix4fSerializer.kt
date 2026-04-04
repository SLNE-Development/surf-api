package dev.slne.surf.api.core.serializer.spongepowered.math.matrix.m4f

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.matrix.Matrix4f

typealias SerializableMatrix4f = @Serializable(with = SpongeMatrix4fSerializer::class) Matrix4f

object SpongeMatrix4fSerializer : KSerializer<Matrix4f> {
    private val arraySerializer = FloatArraySerializer()
    override val descriptor =
        SerialDescriptor("surf.api.sponge.Matrix4f", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Matrix4f,
    ) {
        encoder.encodeSerializableValue(arraySerializer, value.toArray())
    }

    override fun deserialize(decoder: Decoder): Matrix4f {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 16) { "Expected array of size 16, got ${array.size}" }
        return Matrix4f(
            array[0], array[1], array[2], array[3],
            array[4], array[5], array[6], array[7],
            array[8], array[9], array[10], array[11],
            array[12], array[13], array[14], array[15],
        )
    }
}