package dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.date

import com.mojang.serialization.Codec
import java.util.*

object DateCodec {
    val CODEC: Codec<Date> = Codec.LONG.xmap({ Date(it) }, { it.time })
}