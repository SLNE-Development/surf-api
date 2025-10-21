package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3d

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector3d

object SpongeVector3dCodec {
    val CODEC: Codec<Vector3d> = Codec.DOUBLE
        .listOf(3, 3)
        .xmap(
            { coords -> Vector3d.from(coords[0], coords[1], coords[2]) },
            { vector -> listOf(vector.x(), vector.y(), vector.z()) }
        )
}