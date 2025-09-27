package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4f

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector4f

object SpongeVector4fCodec {
    val CODEC: Codec<Vector4f> = Codec.FLOAT
        .listOf(4, 4)
        .xmap(
            { coords -> Vector4f.from(coords[0], coords[1], coords[2], coords[3]) },
            { vector -> listOf(vector.x(), vector.y(), vector.z(), vector.w()) }
        )
}