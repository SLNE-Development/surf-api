package dev.slne.surf.surfapi.core.api.serializer.java.uuid

import com.mojang.serialization.Codec
import dev.slne.surf.surfapi.core.api.serializer.fixedSize
import java.util.*

object JavaUUIDCodec {
    val CODEC: Codec<UUID> = Codec.LONG_STREAM
        .fixedSize(2)
        .xmap({ bytes ->
            UUID(bytes[0], bytes[1])
        }, { uuid ->
            longArrayOf(uuid.mostSignificantBits, uuid.leastSignificantBits)
        })
}