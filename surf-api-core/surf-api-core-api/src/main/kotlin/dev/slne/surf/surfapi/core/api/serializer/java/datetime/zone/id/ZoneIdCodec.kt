package dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.id

import com.mojang.serialization.Codec
import java.time.ZoneId

object ZoneIdCodec {
    val CODEC: Codec<ZoneId> = Codec.STRING.xmap({ ZoneId.of(it) }, { it.id })
}