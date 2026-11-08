package dev.slne.surf.api.core.messages.builder.colors.minecraft

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface DarkPurpleComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.darkPurple(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.DARK_PURPLE, *decoration)

    fun SurfComponentBuilder.darkPurple(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.DARK_PURPLE, *decoration)

    fun SurfComponentBuilder.darkPurple(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.DARK_PURPLE, *decoration)

    fun SurfComponentBuilder.darkPurple(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.DARK_PURPLE, *decoration)
}