package dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.id

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.time.DateTimeException
import java.time.ZoneId
import java.time.zone.ZoneRulesException

object ZoneIdCodec {
    val CODEC: Codec<ZoneId> = Codec.STRING
        .comapFlatMap({ id ->
            try {
                DataResult.success(ZoneId.of(id))
            } catch (e: ZoneRulesException) {
                DataResult.error { "Unknown ZoneID $id: ${e.message}" }
            } catch (e: DateTimeException) {
                DataResult.error { "Invalid ZoneID $id: ${e.message}" }
            }
        }, ZoneId::getId)
}