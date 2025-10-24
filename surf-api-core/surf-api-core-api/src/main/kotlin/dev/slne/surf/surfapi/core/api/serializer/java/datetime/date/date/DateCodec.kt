package dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.date

import com.mojang.serialization.Codec
import dev.slne.surf.surfapi.core.api.serializer.positive
import java.util.*

object DateCodec {
    val CODEC: Codec<Date> = Codec.LONG
        .positive()
        .xmap({ Date(it) }, { it.time })
}