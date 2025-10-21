package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2l

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector2l

object SpongeVector2lCodec {
    val CODEC: Codec<Vector2l> = Codec.LONG
        .listOf(2, 2)
        .xmap(
            { coords -> Vector2l.from(coords[0], coords[1]) },
            { vector -> listOf(vector.x(), vector.y()) }
        )
}