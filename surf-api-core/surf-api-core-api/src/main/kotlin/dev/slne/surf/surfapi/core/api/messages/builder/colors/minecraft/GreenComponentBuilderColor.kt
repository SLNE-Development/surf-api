package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface GreenComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.green(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.GREEN, *decoration)

    fun SurfComponentBuilder.green(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.GREEN, *decoration)

    fun SurfComponentBuilder.green(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.GREEN, *decoration)

    fun SurfComponentBuilder.green(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.GREEN, *decoration)
}