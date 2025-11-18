package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3f

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector3f

object SpongeVector3fCodec {
    val CODEC: Codec<Vector3f> = Codec.FLOAT
        .listOf(3, 3)
        .xmap(
            { coords -> Vector3f.from(coords[0], coords[1], coords[2]) },
            { vector -> listOf(vector.x(), vector.y(), vector.z()) }
        )
}