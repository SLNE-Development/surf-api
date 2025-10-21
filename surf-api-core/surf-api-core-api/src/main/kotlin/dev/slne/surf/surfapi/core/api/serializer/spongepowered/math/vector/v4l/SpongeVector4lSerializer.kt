package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4l

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector4l

typealias SerializableVector4l = @Serializable(with = SpongeVector4lSerializer::class) Vector4l

object SpongeVector4lSerializer : KSerializer<Vector4l> {
    private val arraySerializer = LongArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Vector4l", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector4l,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            longArrayOf(value.x(), value.y(), value.z(), value.w()),
        )
    }

    override fun deserialize(decoder: Decoder): Vector4l {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 4) { "Expected array of size 4, got ${array.size}" }
        return Vector4l.from(array[0], array[1], array[2], array[3])
    }
}