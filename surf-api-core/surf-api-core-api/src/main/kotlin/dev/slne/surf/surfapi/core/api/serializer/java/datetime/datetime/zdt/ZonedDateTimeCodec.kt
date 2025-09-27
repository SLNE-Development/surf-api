package dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.zdt

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.instant.InstantCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.id.ZoneIdCodec
import java.time.ZonedDateTime

object ZonedDateTimeCodec {
    val CODEC: Codec<ZonedDateTime> = RecordCodecBuilder.create { instance ->
        instance.group(
            ZoneIdCodec.CODEC.fieldOf("zoneId").forGetter(ZonedDateTime::getZone),
            InstantCodec.CODEC.fieldOf("instant").forGetter(ZonedDateTime::toInstant)
        ).apply(instance) { zoneId, instant ->
            ZonedDateTime.ofInstant(instant, zoneId)
        }
    }
}