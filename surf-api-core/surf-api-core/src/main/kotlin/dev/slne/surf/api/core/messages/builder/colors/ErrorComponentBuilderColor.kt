package dev.slne.surf.api.core.messages.builder.colors

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface ErrorComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.appendErrorPrefix() = append(Colors.ERROR_PREFIX)
    fun SurfComponentBuilder.appendNewErrorPrefixedLine() = appendNewline().appendErrorPrefix()
    suspend fun SurfComponentBuilder.appendNewErrorPrefixedLineAsync(block: suspend SurfComponentBuilder.() -> Unit) =
        appendNewline().appendErrorPrefix().appendAsync(block)

    fun SurfComponentBuilder.error(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.ERROR, *decoration)

    fun SurfComponentBuilder.error(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.ERROR, *decoration)

    fun SurfComponentBuilder.error(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.ERROR, *decoration)

    fun SurfComponentBuilder.error(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.ERROR, *decoration)
}