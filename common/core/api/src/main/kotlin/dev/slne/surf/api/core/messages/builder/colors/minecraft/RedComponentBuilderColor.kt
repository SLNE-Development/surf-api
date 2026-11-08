package dev.slne.surf.api.core.messages.builder.colors.minecraft

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
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