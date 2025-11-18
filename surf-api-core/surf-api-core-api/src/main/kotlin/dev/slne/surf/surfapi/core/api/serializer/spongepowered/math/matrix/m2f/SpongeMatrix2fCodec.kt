package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m2f

import com.mojang.serialization.Codec
import org.spongepowered.math.matrix.Matrix2f

object SpongeMatrix2fCodec {
    val CODEC: Codec<Matrix2f> = Codec.FLOAT
        .listOf(4, 4)
        .xmap(
            { coords -> Matrix2f.from(coords[0], coords[1], coords[2], coords[3]) },
            { matrix -> matrix.toArray().toList() }
        )
}