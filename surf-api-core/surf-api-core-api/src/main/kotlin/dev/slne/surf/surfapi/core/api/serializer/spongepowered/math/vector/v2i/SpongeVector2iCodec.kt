package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2i

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector2i

object SpongeVector2iCodec {
    val CODEC: Codec<Vector2i> = Codec.INT
        .listOf(2, 2)
        .xmap(
            { coords -> Vector2i.from(coords[0], coords[1]) },
            { vector -> listOf(vector.x(), vector.y()) }
        )
}