package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface DarkAquaComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.darkAqua(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.DARK_AQUA, *decoration)

    fun SurfComponentBuilder.darkAqua(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.DARK_AQUA, *decoration)

    fun SurfComponentBuilder.darkAqua(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.DARK_AQUA, *decoration)

    fun SurfComponentBuilder.darkAqua(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.DARK_AQUA, *decoration)
}