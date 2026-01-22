package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface DarkGreenComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.darkGreen(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.DARK_GREEN, *decoration)

    fun SurfComponentBuilder.darkGreen(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.DARK_GREEN, *decoration)

    fun SurfComponentBuilder.darkGreen(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.DARK_GREEN, *decoration)

    fun SurfComponentBuilder.darkGreen(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.DARK_GREEN, *decoration)
}