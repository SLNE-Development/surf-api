package dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnl

import com.mojang.serialization.Codec
import org.spongepowered.math.vector.VectorNl

object SpongeVectorNlCodec {
    val CODEC: Codec<VectorNl> = Codec.LONG.listOf(2, Int.MAX_VALUE)
        .xmap(
            { coords -> VectorNl(*coords.toLongArray()) },
            { vector -> vector.toArray().toList() }
        )
}