package dev.slne.surf.surfapi.core.api.serializer.adventure.component.shadowcolor

import com.mojang.serialization.Codec
import net.kyori.adventure.text.format.ShadowColor

object AdventureShadowColorCodec {
    val CODEC: Codec<ShadowColor> = Codec.INT.xmap(ShadowColor::shadowColor, ShadowColor::value)
}