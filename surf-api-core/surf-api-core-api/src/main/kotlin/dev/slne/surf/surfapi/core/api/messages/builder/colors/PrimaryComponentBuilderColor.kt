package dev.slne.surf.surfapi.core.api.messages.builder.colors

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface PrimaryComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.primary(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.PRIMARY, *decoration)

    fun SurfComponentBuilder.primary(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.PRIMARY, *decoration)

    fun SurfComponentBuilder.primary(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.PRIMARY, *decoration)

    fun SurfComponentBuilder.primary(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.PRIMARY, *decoration)
}