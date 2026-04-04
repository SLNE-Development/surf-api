package dev.slne.surf.api.core.messages.builder.colors.minecraft

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface DarkRedComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.darkRed(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.DARK_RED, *decoration)

    fun SurfComponentBuilder.darkRed(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.DARK_RED, *decoration)

    fun SurfComponentBuilder.darkRed(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.DARK_RED, *decoration)

    fun SurfComponentBuilder.darkRed(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.DARK_RED, *decoration)
}