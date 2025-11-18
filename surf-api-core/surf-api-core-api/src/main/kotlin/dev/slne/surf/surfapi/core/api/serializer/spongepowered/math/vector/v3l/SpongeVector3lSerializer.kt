package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3l

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.Vector3l

typealias SerializableVector3l = @Serializable(with = SpongeVector3lSerializer::class) Vector3l

object SpongeVector3lSerializer : KSerializer<Vector3l> {
    private val arraySerializer = LongArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.Vector3l", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Vector3l,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            longArrayOf(value.x(), value.y(), value.z())
        )
    }

    override fun deserialize(decoder: Decoder): Vector3l {
        val (x, y, z) = decoder.decodeSerializableValue(arraySerializer)
        return Vector3l(x, y, z)
    }
}