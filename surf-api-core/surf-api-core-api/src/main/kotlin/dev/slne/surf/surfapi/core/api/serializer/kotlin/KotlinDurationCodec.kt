package dev.slne.surf.surfapi.core.api.serializer.kotlin

import com.mojang.serialization.Codec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object KotlinDurationCodec {
    val CODEC: Codec<Duration> = Codec.LONG.xmap(
        { it.milliseconds },
        { it.inWholeMilliseconds }
    )
}