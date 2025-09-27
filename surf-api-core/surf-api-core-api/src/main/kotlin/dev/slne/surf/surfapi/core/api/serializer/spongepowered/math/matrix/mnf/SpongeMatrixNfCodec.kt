package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.mnf

import com.mojang.serialization.Codec
import org.spongepowered.math.matrix.MatrixNf

object SpongeMatrixNfCodec {
    val CODEC: Codec<MatrixNf> = Codec.FLOAT
        .listOf(4, Int.MAX_VALUE)
        .xmap(
            { coords -> MatrixNf(*coords.toFloatArray()) },
            { matrix -> matrix.toArray().toList() }
        )
}