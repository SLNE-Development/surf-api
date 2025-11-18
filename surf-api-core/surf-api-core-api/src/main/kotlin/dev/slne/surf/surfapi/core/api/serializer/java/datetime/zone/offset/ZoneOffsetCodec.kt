package dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.offset

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.time.DateTimeException
import java.time.ZoneOffset

object ZoneOffsetCodec {
    val CODEC: Codec<ZoneOffset> = Codec.STRING
        .comapFlatMap({ id ->
            try {
                DataResult.success(ZoneOffset.of(id))
            } catch (e: DateTimeException) {
                DataResult.error { "Invalid ZoneOffset $id: ${e.message}" }
            }

        }, ZoneOffset::getId)
}