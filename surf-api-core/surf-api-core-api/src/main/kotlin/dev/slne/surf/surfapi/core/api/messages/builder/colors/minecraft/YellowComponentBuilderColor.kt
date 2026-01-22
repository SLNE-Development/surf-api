package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface YellowComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.yellow(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.YELLOW, *decoration)

    fun SurfComponentBuilder.yellow(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.YELLOW, *decoration)

    fun SurfComponentBuilder.yellow(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.YELLOW, *decoration)

    fun SurfComponentBuilder.yellow(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.YELLOW, *decoration)
}