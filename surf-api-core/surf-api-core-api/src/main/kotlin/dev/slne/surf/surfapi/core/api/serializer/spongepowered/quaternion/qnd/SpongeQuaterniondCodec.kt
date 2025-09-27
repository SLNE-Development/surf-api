package dev.slne.surf.surfapi.core.api.serializer.spongepowered.quaternion.qnd

import com.mojang.serialization.Codec
import org.spongepowered.math.imaginary.Quaterniond

object SpongeQuaterniondCodec {
    val CODEC: Codec<Quaterniond> = Codec.DOUBLE
        .listOf(4, 4)
        .xmap(
            { coords -> Quaterniond(coords[0], coords[1], coords[2], coords[3]) },
            { quaternion -> listOf(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w()) }
        )
}