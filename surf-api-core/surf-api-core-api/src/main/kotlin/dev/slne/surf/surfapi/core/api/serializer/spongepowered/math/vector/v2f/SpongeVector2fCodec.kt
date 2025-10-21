package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2f

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector2f

object SpongeVector2fCodec {
    val CODEC: Codec<Vector2f> = Codec.FLOAT
        .listOf(2, 2)
        .xmap(
            { coords -> Vector2f.from(coords[0], coords[1]) },
            { vector -> listOf(vector.x(), vector.y()) }
        )
}