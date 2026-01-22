package dev.slne.surf.surfapi.core.api.messages.builder.colors

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface SpacerComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.spacer(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.SPACER, *decoration)

    fun SurfComponentBuilder.spacer(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.SPACER, *decoration)

    fun SurfComponentBuilder.spacer(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.SPACER, *decoration)

    fun SurfComponentBuilder.spacer(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.SPACER, *decoration)
}