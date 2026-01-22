package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface WhiteComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.white(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.WHITE, *decoration)

    fun SurfComponentBuilder.white(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.WHITE, *decoration)

    fun SurfComponentBuilder.white(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.WHITE, *decoration)

    fun SurfComponentBuilder.white(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.WHITE, *decoration)
}