package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m4f

import com.mojang.serialization.Codec
import org.spongepowered.math.matrix.Matrix4f

object SpongeMatrix4fCodec {
    val CODEC: Codec<Matrix4f> = Codec.FLOAT
        .listOf(16, 16)
        .xmap(
            { coords ->
                Matrix4f.from(
                    coords[0], coords[1], coords[2], coords[3],
                    coords[4], coords[5], coords[6], coords[7],
                    coords[8], coords[9], coords[10], coords[11],
                    coords[12], coords[13], coords[14], coords[15],
                )
            },
            { matrix -> matrix.toArray().toList() }
        )
}