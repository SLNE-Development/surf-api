package dev.slne.surf.surfapi.core.api.messages.builder.colors

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface ErrorComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.appendErrorPrefix() = append(Colors.ERROR_PREFIX)
    fun SurfComponentBuilder.appendNewErrorPrefixedLine() = appendNewline().appendErrorPrefix()

    fun SurfComponentBuilder.error(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.ERROR, *decoration)

    fun SurfComponentBuilder.error(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.ERROR, *decoration)

    fun SurfComponentBuilder.error(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.ERROR, *decoration)

    fun SurfComponentBuilder.error(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.ERROR, *decoration)
}