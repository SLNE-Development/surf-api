package dev.slne.surf.surfapi.core.api.serializer.adventure.component.decoration

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.kyori.adventure.text.format.TextDecoration

object AdventureTextDecorationCodec {
    val CODEC: Codec<TextDecoration> = Codec.STRING.comapFlatMap({ str ->
        val decoration = TextDecoration.NAMES.value(str.uppercase())

        if (decoration != null) {
            DataResult.success(decoration)
        } else {
            DataResult.error { "Unknown text decoration: $str" }
        }
    }, { it.toString() })
}