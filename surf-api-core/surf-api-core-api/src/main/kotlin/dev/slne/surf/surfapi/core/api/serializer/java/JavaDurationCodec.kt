package dev.slne.surf.surfapi.core.api.serializer.java

import com.mojang.serialization.Codec
import java.time.Duration

object JavaDurationCodec {
    val CODEC: Codec<Duration> = Codec.LONG.xmap(
        { Duration.ofMillis(it) },
        { it.toMillis() }
    )
}