package dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.instant

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.time.DateTimeException
import java.time.Instant

object InstantCodec {
    val CODEC: Codec<Instant> = Codec.LONG
        .comapFlatMap({ epochMilli ->
            try {
                DataResult.success(Instant.ofEpochMilli(epochMilli))
            } catch (e: DateTimeException) {
                DataResult.error { "Invalid instant $epochMilli: ${e.message}" }
            }
        }, Instant::toEpochMilli)
}