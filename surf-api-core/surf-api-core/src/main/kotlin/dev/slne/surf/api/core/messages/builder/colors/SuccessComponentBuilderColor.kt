package dev.slne.surf.api.core.messages.builder.colors

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface SuccessComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.appendSuccessPrefix() = append(Colors.SUCCESS_PREFIX)
    
    fun SurfComponentBuilder.appendNewSuccessPrefixedLine() = appendNewline().appendSuccessPrefix()
    fun SurfComponentBuilder.appendNewSuccessPrefixedLine(block: SurfComponentBuilder.() -> Unit) =
        appendNewline().appendSuccessPrefix().append(block)

    suspend fun SurfComponentBuilder.appendNewSuccessPrefixedLineAsync(block: suspend SurfComponentBuilder.() -> Unit) =
        appendNewline().appendSuccessPrefix().appendAsync(block)

    fun SurfComponentBuilder.success(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.SUCCESS, *decoration)

    fun SurfComponentBuilder.success(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.SUCCESS, *decoration)

    fun SurfComponentBuilder.success(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.SUCCESS, *decoration)

    fun SurfComponentBuilder.success(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.SUCCESS, *decoration)
}