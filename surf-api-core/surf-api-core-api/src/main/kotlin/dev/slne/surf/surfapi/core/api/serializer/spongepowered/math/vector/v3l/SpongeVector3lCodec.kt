package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3l

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector3l

object SpongeVector3lCodec {
    val CODEC: Codec<Vector3l> = Codec.LONG
        .listOf(3, 3)
        .xmap(
            { coords -> Vector3l.from(coords[0], coords[1], coords[2]) },
            { vector -> listOf(vector.x(), vector.y(), vector.z()) }
        )
}