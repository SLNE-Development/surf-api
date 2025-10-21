package dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.local

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.time.DateTimeException
import java.time.LocalDate

object LocalDateCodec {
    val CODEC: Codec<LocalDate> = Codec.LONG
        .comapFlatMap({ epochDay ->
            try {
                DataResult.success(LocalDate.ofEpochDay(epochDay))
            } catch (e: DateTimeException) {
                DataResult.error { "Invalid date $epochDay: ${e.message}" }
            }
        }, LocalDate::toEpochDay)
}