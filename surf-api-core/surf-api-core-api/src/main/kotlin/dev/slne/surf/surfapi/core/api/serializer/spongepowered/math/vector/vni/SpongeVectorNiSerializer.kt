package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vni

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.VectorNi

typealias SerializableVectorNi = @Serializable(with = SpongeVectorNiSerializer::class) VectorNi

object SpongeVectorNiSerializer : KSerializer<VectorNi> {
    private val arraySerializer = IntArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.VectorNi", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: VectorNi,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            value.toArray(),
        )
    }

    override fun deserialize(decoder: Decoder): VectorNi {
        val array = decoder.decodeSerializableValue(arraySerializer)
        return VectorNi(*array)
    }
}