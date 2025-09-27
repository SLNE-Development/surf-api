package dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.instant

import com.mojang.serialization.Codec
import java.time.Instant

object InstantCodec {
    val CODEC: Codec<Instant> = Codec.LONG.xmap({ Instant.ofEpochMilli(it) }, { it.toEpochMilli() })
}