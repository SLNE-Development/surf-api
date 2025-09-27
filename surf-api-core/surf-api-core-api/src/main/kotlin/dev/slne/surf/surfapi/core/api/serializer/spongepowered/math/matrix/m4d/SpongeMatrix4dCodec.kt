package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m4d

import com.mojang.serialization.Codec
import org.spongepowered.math.matrix.Matrix4d

object SpongeMatrix4dCodec {
    val CODEC: Codec<Matrix4d> = Codec.DOUBLE
        .listOf(16, 16)
        .xmap(
            { coords ->
                Matrix4d.from(
                    coords[0], coords[1], coords[2], coords[3],
                    coords[4], coords[5], coords[6], coords[7],
                    coords[8], coords[9], coords[10], coords[11],
                    coords[12], coords[13], coords[14], coords[15],
                )
            },
            { matrix -> matrix.toArray().toList() }
        )
}