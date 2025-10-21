package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4d

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector4d

typealias SerializableVector4d = @Serializable(with = SpongeVector4dSerializer::class) Vector4d

object SpongeVector4dSerializer : KSerializer<Vector4d> {
    private val arraySerializer = DoubleArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Vector4d", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector4d,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            doubleArrayOf(value.x(), value.y(), value.z(), value.w()),
        )
    }

    override fun deserialize(decoder: Decoder): Vector4d {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 4) { "Expected array of size 4, got ${array.size}" }
        return Vector4d.from(array[0], array[1], array[2], array[3])
    }
}