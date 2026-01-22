package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface GrayComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.gray(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.GRAY, *decoration)

    fun SurfComponentBuilder.gray(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.GRAY, *decoration)

    fun SurfComponentBuilder.gray(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.GRAY, *decoration)

    fun SurfComponentBuilder.gray(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.GRAY, *decoration)
}