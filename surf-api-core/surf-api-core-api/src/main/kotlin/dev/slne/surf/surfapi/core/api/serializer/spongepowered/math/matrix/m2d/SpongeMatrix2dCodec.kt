package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m2d

import com.mojang.serialization.Codec
import org.spongepowered.math.matrix.Matrix2d

object SpongeMatrix2dCodec {
    val CODEC: Codec<Matrix2d> = Codec.DOUBLE
        .listOf(4, 4)
        .xmap(
            { coords -> Matrix2d.from(coords[0], coords[1], coords[2], coords[3]) },
            { matrix -> matrix.toArray().toList() }
        )
}