package dev.slne.surf.api.core.messages.builder.colors

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface DarkSpacerComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.darkSpacer(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.DARK_SPACER, *decoration)

    fun SurfComponentBuilder.darkSpacer(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.DARK_SPACER, *decoration)

    fun SurfComponentBuilder.darkSpacer(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.DARK_SPACER, *decoration)

    fun SurfComponentBuilder.darkSpacer(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.DARK_SPACER, *decoration)
}