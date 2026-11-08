package dev.slne.surf.api.core.messages.builder.colors

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface SecondaryComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.secondary(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.SECONDARY, *decoration)

    fun SurfComponentBuilder.secondary(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.SECONDARY, *decoration)

    fun SurfComponentBuilder.secondary(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.SECONDARY, *decoration)

    fun SurfComponentBuilder.secondary(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.SECONDARY, *decoration)
}