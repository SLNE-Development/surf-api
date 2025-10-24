package dev.slne.surf.surfapi.core.api.serializer.adventure.component.textcolor

import com.mojang.serialization.Codec
import net.kyori.adventure.text.format.TextColor

object AdventureTextColorCodec {
    val CODEC: Codec<TextColor> = Codec.INT.xmap(TextColor::color, TextColor::value)
}