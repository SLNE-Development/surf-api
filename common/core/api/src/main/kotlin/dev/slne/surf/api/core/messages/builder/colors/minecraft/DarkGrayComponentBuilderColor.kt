package dev.slne.surf.api.core.messages.builder.colors.minecraft

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface DarkGrayComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.darkGray(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.DARK_GRAY, *decoration)

    fun SurfComponentBuilder.darkGray(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.DARK_GRAY, *decoration)

    fun SurfComponentBuilder.darkGray(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.DARK_GRAY, *decoration)

    fun SurfComponentBuilder.darkGray(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.DARK_GRAY, *decoration)
}