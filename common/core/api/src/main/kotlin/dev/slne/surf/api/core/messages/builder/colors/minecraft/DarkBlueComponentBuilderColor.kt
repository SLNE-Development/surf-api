package dev.slne.surf.api.core.messages.builder.colors.minecraft

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface DarkBlueComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.darkBlue(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.DARK_BLUE, *decoration)

    fun SurfComponentBuilder.darkBlue(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.DARK_BLUE, *decoration)

    fun SurfComponentBuilder.darkBlue(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.DARK_BLUE, *decoration)

    fun SurfComponentBuilder.darkBlue(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.DARK_BLUE, *decoration)
}