package dev.slne.surf.surfapi.core.api.messages.builder.colors

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface SuccessComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.appendSuccessPrefix() = append(Colors.SUCCESS_PREFIX)
    fun SurfComponentBuilder.appendNewSuccessPrefixedLine() = appendNewline().appendSuccessPrefix()

    fun SurfComponentBuilder.success(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.SUCCESS, *decoration)

    fun SurfComponentBuilder.success(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.SUCCESS, *decoration)

    fun SurfComponentBuilder.success(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.SUCCESS, *decoration)

    fun SurfComponentBuilder.success(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.SUCCESS, *decoration)
}