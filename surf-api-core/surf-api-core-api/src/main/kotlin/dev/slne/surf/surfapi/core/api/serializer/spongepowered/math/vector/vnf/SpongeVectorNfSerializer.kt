package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.VectorNf

typealias SerializableVectorNf = @Serializable(with = SpongeVectorNfSerializer::class) VectorNf

object SpongeVectorNfSerializer : KSerializer<VectorNf> {
    private val arraySerializer = FloatArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.VectorNf", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: VectorNf,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            value.toArray(),
        )
    }

    override fun deserialize(decoder: Decoder): VectorNf {
        val array = decoder.decodeSerializableValue(arraySerializer)
        return VectorNf(*array)
    }
}