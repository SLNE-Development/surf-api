package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.vector.VectorNl

typealias SerializableVectorNl = @Serializable(with = SpongeVectorNlSerializer::class) VectorNl

object SpongeVectorNlSerializer : KSerializer<VectorNl> {
    private val arraySerializer = LongArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.VectorNl", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: VectorNl,
    ) {
        encoder.encodeSerializableValue(
            arraySerializer,
            value.toArray(),
        )
    }

    override fun deserialize(decoder: Decoder): VectorNl {
        val array = decoder.decodeSerializableValue(arraySerializer)
        return VectorNl(*array)
    }
}