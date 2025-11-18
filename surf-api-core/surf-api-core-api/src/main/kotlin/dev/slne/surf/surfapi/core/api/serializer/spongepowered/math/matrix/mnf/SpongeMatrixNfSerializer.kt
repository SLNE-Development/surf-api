package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.mnf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.matrix.MatrixNf

typealias SerializableMatrixNf = @Serializable(with = SpongeMatrixNfSerializer::class) MatrixNf

object SpongeMatrixNfSerializer : KSerializer<MatrixNf> {
    private val arraySerializer = FloatArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.MatrixNf", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: MatrixNf,
    ) {
        encoder.encodeSerializableValue(arraySerializer, value.toArray())
    }

    override fun deserialize(decoder: Decoder): MatrixNf {
        val decodeSerializableValue = decoder.decodeSerializableValue(arraySerializer)
        return MatrixNf(*decodeSerializableValue)
    }
}