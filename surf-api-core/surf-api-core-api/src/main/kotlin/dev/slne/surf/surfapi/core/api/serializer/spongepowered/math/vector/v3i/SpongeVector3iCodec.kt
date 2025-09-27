package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3i

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector3i

object SpongeVector3iCodec {
    val CODEC: Codec<Vector3i> = Codec.INT
        .listOf(3, 3)
        .xmap(
            { coords -> Vector3i.from(coords[0], coords[1], coords[2]) },
            { vector -> listOf(vector.x(), vector.y(), vector.z()) }
        )
}