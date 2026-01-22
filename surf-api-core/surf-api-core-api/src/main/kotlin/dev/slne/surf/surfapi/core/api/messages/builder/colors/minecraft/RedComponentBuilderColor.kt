package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface RedComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.red(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.RED, *decoration)

    fun SurfComponentBuilder.red(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.RED, *decoration)

    fun SurfComponentBuilder.red(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.RED, *decoration)

    fun SurfComponentBuilder.red(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.RED, *decoration)
}