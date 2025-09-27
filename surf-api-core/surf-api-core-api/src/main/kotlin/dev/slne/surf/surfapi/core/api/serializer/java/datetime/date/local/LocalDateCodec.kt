package dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.local

import com.mojang.serialization.Codec
import java.time.LocalDate

object LocalDateCodec {
    val CODEC: Codec<LocalDate> = Codec.LONG.xmap({ LocalDate.ofEpochDay(it) }, { it.toEpochDay() })
}