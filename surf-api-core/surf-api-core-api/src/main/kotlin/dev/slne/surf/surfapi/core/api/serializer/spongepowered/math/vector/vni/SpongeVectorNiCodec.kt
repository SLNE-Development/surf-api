package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vni

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.VectorNi

object SpongeVectorNiCodec {
    val CODEC: Codec<VectorNi> = Codec.INT.listOf(2, Int.MAX_VALUE)
        .xmap(
            { coords -> VectorNi(*coords.toIntArray()) },
            { vector -> vector.toArray().toList() }
        )
}