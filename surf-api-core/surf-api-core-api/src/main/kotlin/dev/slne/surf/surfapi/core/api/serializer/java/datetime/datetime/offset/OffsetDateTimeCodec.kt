package dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.offset

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.ldt.LocalDateTimeCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.offset.ZoneOffsetCodec
import java.time.OffsetDateTime

object OffsetDateTimeCodec {
    val CODEC: Codec<OffsetDateTime> = RecordCodecBuilder.create { instance ->
        instance.group(
            ZoneOffsetCodec.CODEC.fieldOf("zoneOffset").forGetter(OffsetDateTime::getOffset),
            LocalDateTimeCodec.CODEC.fieldOf("localDateTime")
                .forGetter(OffsetDateTime::toLocalDateTime)
        ).apply(instance) { offset, localDateTime ->
            OffsetDateTime.of(localDateTime, offset)
        }
    }
}