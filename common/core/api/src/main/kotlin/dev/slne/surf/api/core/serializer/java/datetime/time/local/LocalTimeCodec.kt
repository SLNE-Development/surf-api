package dev.slne.surf.api.core.serializer.java.datetime.time.local

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.time.DateTimeException
import java.time.LocalTime

object LocalTimeCodec {
    val CODEC: Codec<LocalTime> = Codec.LONG
        .comapFlatMap({ nanoOfDay ->
            try {
                DataResult.success(LocalTime.ofNanoOfDay(nanoOfDay))
            } catch (e: DateTimeException) {
                DataResult.error { "Invalid time $nanoOfDay: ${e.message}" }
            }
        }, LocalTime::toNanoOfDay)
}