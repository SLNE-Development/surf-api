package dev.slne.surf.surfapi.core.api.messages.builder.colors

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface InfoComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.appendInfoPrefix() = append(Colors.INFO_PREFIX)
    fun SurfComponentBuilder.appendNewInfoPrefixedLine() = appendNewline().appendInfoPrefix()
    suspend fun SurfComponentBuilder.appendNewInfoPrefixedLineAsync(block: suspend SurfComponentBuilder.() -> Unit) =
        appendNewline().appendInfoPrefix().appendAsync(block)

    fun SurfComponentBuilder.info(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.INFO, *decoration)

    fun SurfComponentBuilder.info(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.INFO, *decoration)

    fun SurfComponentBuilder.info(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.INFO, *decoration)

    fun SurfComponentBuilder.info(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.INFO, *decoration)
}