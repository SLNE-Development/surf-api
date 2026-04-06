package dev.slne.surf.api.core.serializer.spongepowered.math.vector.v4i

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector4i

typealias SerializableVector4i = @Serializable(with = SpongeVector4iSerializer::class) Vector4i

object SpongeVector4iSerializer : KSerializer<Vector4i> {
    private val arraySerializer = IntArraySerializer()
    override val descriptor =
        SerialDescriptor("surf.api.sponge.Vector4i", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector4i,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            intArrayOf(value.x(), value.y(), value.z(), value.w()),
        )
    }

    override fun deserialize(decoder: Decoder): Vector4i {
        val array = decoder.decodeSerializableValue(arraySerializer)
        require(array.size == 4) { "Expected array of size 4, got ${array.size}" }
        return Vector4i.from(array[0], array[1], array[2], array[3])
    }
}