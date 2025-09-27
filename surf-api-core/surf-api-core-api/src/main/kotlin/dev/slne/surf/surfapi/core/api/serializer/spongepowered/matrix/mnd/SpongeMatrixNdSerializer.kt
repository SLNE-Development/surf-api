package dev.slne.surf.surfapi.core.api.serializer.spongepowered.matrix.mnd

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spongepowered.math.matrix.MatrixNd

typealias SerializableMatrixNd = @Serializable(with = SpongeMatrixNdSerializer::class) MatrixNd

object SpongeMatrixNdSerializer : KSerializer<MatrixNd> {
    private val arraySerializer = DoubleArraySerializer()
    override val descriptor =
        SerialDescriptor("surfapi.sponge.MatrixNd", arraySerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: MatrixNd,
    ) {
        encoder.encodeSerializableValue(arraySerializer, value.toArray())
    }

    override fun deserialize(decoder: Decoder): MatrixNd {
        val decodeSerializableValue = decoder.decodeSerializableValue(arraySerializer)
        return MatrixNd(*decodeSerializableValue)
    }
}