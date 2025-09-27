package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m3f

import com.mojang.serialization.Codec
import org.spongepowered.math.matrix.Matrix3f

object SpongeMatrix3fCodec {
    val CODEC: Codec<Matrix3f> = Codec.FLOAT
        .listOf(9, 9)
        .xmap(
            { coords ->
                Matrix3f.from(
                    coords[0], coords[1], coords[2],
                    coords[3], coords[4], coords[5],
                    coords[6], coords[7], coords[8],
                )
            },
            { matrix -> matrix.toArray().toList() }
        )
}