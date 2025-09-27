package dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.offset

import com.mojang.serialization.Codec
import java.time.ZoneOffset

object ZoneOffsetCodec {
    val CODEC: Codec<ZoneOffset> = Codec.STRING.xmap({ ZoneOffset.of(it) }, { it.id })
}