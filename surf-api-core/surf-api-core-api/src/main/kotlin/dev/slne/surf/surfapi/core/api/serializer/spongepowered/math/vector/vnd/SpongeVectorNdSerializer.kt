package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnd

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.VectorNd

typealias SerializableVectorNd = @Serializable(with = SpongeVectorNdSerializer::class) VectorNd

object SpongeVectorNdSerializer : KSerializer<VectorNd> {
    private val arraySerializer = DoubleArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.VectorNd", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: VectorNd,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            value.toArray(),
        )
    }

    override fun deserialize(decoder: Decoder): VectorNd {
        val array = decoder.decodeSerializableValue(arraySerializer)
        return VectorNd(*array)
    }
}