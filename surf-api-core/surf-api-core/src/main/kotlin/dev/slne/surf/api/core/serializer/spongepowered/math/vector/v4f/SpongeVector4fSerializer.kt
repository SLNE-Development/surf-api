package dev.slne.surf.api.core.serializer.spongepowered.math.vector.v4f

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector4f

typealias SerializableVector4f = @Serializable(with = SpongeVector4fSerializer::class) Vector4f

object SpongeVector4fSerializer : KSerializer<Vector4f> {
    private val arraySerializer = FloatArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Vector4f", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector4f,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            floatArrayOf(value.x(), value.y(), value.z(), value.w()),
        )
    }

    override fun deserialize(decoder: Decoder): Vector4f {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 4) { "Expected array of size 4, got ${array.size}" }
        return Vector4f.from(array[0], array[1], array[2], array[3])
    }
}