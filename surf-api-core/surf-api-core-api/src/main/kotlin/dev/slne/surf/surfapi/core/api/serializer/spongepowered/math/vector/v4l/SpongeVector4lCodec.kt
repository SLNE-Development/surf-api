package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4l

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector4l

object SpongeVector4lCodec {
    val CODEC: Codec<Vector4l> = Codec.LONG
        .listOf(4, 4)
        .xmap(
            { coords -> Vector4l.from(coords[0], coords[1], coords[2], coords[3]) },
            { vector -> listOf(vector.x(), vector.y(), vector.z(), vector.w()) }
        )
}