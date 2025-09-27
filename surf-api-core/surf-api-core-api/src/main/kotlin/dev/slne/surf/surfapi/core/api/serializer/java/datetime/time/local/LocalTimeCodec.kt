package dev.slne.surf.surfapi.core.api.serializer.java.datetime.time.local

import com.mojang.serialization.Codec
import java.time.LocalTime

object LocalTimeCodec {
    val CODEC: Codec<LocalTime> =
        Codec.LONG.xmap({ LocalTime.ofNanoOfDay(it) }, { it.toNanoOfDay() })
}