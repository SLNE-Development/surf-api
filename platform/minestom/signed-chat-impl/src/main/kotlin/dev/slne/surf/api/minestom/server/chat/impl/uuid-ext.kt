package dev.slne.surf.api.minestom.server.chat.impl

import com.google.common.primitives.Longs
import java.util.*

internal val NIL_UUID: UUID = UUID(0L, 0L)

internal fun UUID.toByteArray(): ByteArray =
    Longs.toByteArray(mostSignificantBits) + Longs.toByteArray(leastSignificantBits)
