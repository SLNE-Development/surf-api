package dev.slne.surf.api.core.serializer.spongepowered.math.vector.v3i

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector3i

typealias SerializableVector3i = @Serializable(with = SpongeVector3iSerializer::class) Vector3i

object SpongeVector3iSerializer : KSerializer<Vector3i> {
    private val arraySerializer = IntArraySerializer()
    override val descriptor =
        SerialDescriptor("surf.api.sponge.Vector3i", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector3i,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            intArrayOf(value.x(), value.y(), value.z())
        )
    }

    override fun deserialize(decoder: Decoder): Vector3i {
        val (x, y, z) = decoder.decodeSerializableValue(arraySerializer)
        return Vector3i(x, y, z)
    }
}