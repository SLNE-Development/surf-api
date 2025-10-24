package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4d

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector4d

object SpongeVector4dCodec {
    val CODEC: Codec<Vector4d> = Codec.DOUBLE
        .listOf(4, 4)
        .xmap(
            { coords -> Vector4d.from(coords[0], coords[1], coords[2], coords[3]) },
            { vector -> listOf(vector.x(), vector.y(), vector.z(), vector.w()) }
        )
}