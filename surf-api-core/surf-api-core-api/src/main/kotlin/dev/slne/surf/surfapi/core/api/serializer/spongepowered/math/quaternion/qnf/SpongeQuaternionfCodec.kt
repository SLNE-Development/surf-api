package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.quaternion.qnf

import com.mojang.serialization.Codec
import org.spongepowered.math.imaginary.Quaternionf

object SpongeQuaternionfCodec {
    val CODEC: Codec<Quaternionf> = Codec.FLOAT
        .listOf(4, 4)
        .xmap(
            { coords -> Quaternionf(coords[0], coords[1], coords[2], coords[3]) },
            { quaternion -> listOf(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w()) }
        )
}