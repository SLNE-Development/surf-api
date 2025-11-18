package dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.ldt

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.local.LocalDateCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.time.local.LocalTimeCodec
import java.time.LocalDateTime

object LocalDateTimeCodec {
    val CODEC: Codec<LocalDateTime> = RecordCodecBuilder.create { instance ->
        instance.group(
            LocalDateCodec.CODEC.fieldOf("localDate").forGetter(LocalDateTime::toLocalDate),
            LocalTimeCodec.CODEC.fieldOf("localTime").forGetter(LocalDateTime::toLocalTime)
        ).apply(instance, LocalDateTime::of)
    }
}