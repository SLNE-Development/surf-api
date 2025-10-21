package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2d

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector2d

object SpongeVector2dCodec {
    val CODEC: Codec<Vector2d> = Codec.DOUBLE
        .listOf(2, 2)
        .xmap(
            { coords -> Vector2d.from(coords[0], coords[1]) },
            { vector -> listOf(vector.x(), vector.y()) }
        )
}