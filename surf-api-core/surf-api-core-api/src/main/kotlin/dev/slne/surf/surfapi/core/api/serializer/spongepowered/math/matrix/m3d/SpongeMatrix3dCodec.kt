package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m3d

import com.mojang.serialization.Codec
import org.spongepowered.math.matrix.Matrix3d

object SpongeMatrix3dCodec {
    val CODEC: Codec<Matrix3d> = Codec.DOUBLE
        .listOf(9, 9)
        .xmap(
            { coords ->
                Matrix3d.from(
                    coords[0], coords[1], coords[2],
                    coords[3], coords[4], coords[5],
                    coords[6], coords[7], coords[8],
                )
            },
            { matrix -> matrix.toArray().toList() }
        )
}