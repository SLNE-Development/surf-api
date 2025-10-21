package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnd

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.VectorNd

object SpongeVectorNdCodec {
    val CODEC: Codec<VectorNd> = Codec.DOUBLE.listOf(2, Int.MAX_VALUE)
        .xmap(
            { coords -> VectorNd(*coords.toDoubleArray()) },
            { vector -> vector.toArray().toList() }
        )
}