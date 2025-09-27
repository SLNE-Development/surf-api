package dev.slne.surf.surfapi.core.api.serializer.spongepowered.matrix.mnd

import com.mojang.serialization.Codec
import org.spongepowered.math.matrix.MatrixNd

object SpongeMatrixNdCodec {
    val CODEC: Codec<MatrixNd> = Codec.DOUBLE
        .listOf(4, Int.MAX_VALUE)
        .xmap(
            { coords -> MatrixNd(*coords.toDoubleArray()) },
            { matrix -> matrix.toArray().toList() }
        )
}