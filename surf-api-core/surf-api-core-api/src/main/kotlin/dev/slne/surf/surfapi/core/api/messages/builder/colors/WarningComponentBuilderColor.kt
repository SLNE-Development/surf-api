package dev.slne.surf.surfapi.core.api.messages.builder.colors

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface WarningComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.appendWarningPrefix() = append(Colors.WARNING_PREFIX)
    fun SurfComponentBuilder.appendNewWarningPrefixedLine() = appendNewline().appendWarningPrefix()
    suspend fun SurfComponentBuilder.appendNewWarningPrefixedLineAsync(block: suspend SurfComponentBuilder.() -> Unit) =
        appendNewline().appendWarningPrefix().appendAsync(block)

    fun SurfComponentBuilder.warning(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.WARNING, *decoration)

    fun SurfComponentBuilder.warning(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.WARNING, *decoration)

    fun SurfComponentBuilder.warning(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.WARNING, *decoration)

    fun SurfComponentBuilder.warning(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.WARNING, *decoration)
}