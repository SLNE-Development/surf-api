package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4i

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.Vector4i

object SpongeVector4iCodec {
    val CODEC: Codec<Vector4i> = Codec.INT
        .listOf(4, 4)
        .xmap(
            { coords -> Vector4i.from(coords[0], coords[1], coords[2], coords[3]) },
            { vector -> listOf(vector.x(), vector.y(), vector.z(), vector.w()) }
        )
}