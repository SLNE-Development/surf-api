package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnf

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.VectorNf

object SpongeVectorNfCodec {
    val CODEC: Codec<VectorNf> = Codec.FLOAT.listOf(2, Int.MAX_VALUE)
        .xmap(
            { coords -> VectorNf(*coords.toFloatArray()) },
            { vector -> vector.toArray().toList() }
        )
}