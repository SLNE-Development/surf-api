package dev.slne.surf.api.core.serializer.spongepowered.math.vector.v3f

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector3f

typealias SerializableVector3f = @Serializable(with = SpongeVector3fSerializer::class) Vector3f

object SpongeVector3fSerializer : KSerializer<Vector3f> {
    private val arraySerializer = FloatArraySerializer()
    override val descriptor =
        SerialDescriptor("surf.api.sponge.Vector3f", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector3f,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            floatArrayOf(value.x(), value.y(), value.z())
        )
    }

    override fun deserialize(decoder: Decoder): Vector3f {
        val (x, y, z) = decoder.decodeSerializableValue(arraySerializer)
        return Vector3f(x, y, z)
    }
}