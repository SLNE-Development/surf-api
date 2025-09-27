package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3d

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector3d

typealias SerializableVector3d = @Serializable(with = SpongeVector3dSerializer::class) Vector3d

object SpongeVector3dSerializer : KSerializer<Vector3d> {
    private val arraySerializer = DoubleArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Vector3d", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector3d,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            doubleArrayOf(value.x(), value.y(), value.z())
        )
    }

    override fun deserialize(decoder: Decoder): Vector3d {
        val (x, y, z) = decoder.decodeSerializableValue(arraySerializer)
        return Vector3d(x, y, z)
    }
}