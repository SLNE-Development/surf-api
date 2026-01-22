package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface BlackComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.black(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.BLACK, *decoration)

    fun SurfComponentBuilder.black(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.BLACK, *decoration)

    fun SurfComponentBuilder.black(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.BLACK, *decoration)

    fun SurfComponentBuilder.black(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.BLACK, *decoration)
}