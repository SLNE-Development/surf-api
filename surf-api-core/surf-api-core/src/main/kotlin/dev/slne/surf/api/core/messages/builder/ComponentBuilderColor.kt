package dev.slne.surf.api.core.messages.builder

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

interface ComponentBuilderColor {
    fun SurfComponentBuilder.coloredComponent(
        text: String,
        color: TextColor,
        vararg decoration: TextDecoration
    ) = text(text, color, *decoration)

    fun SurfComponentBuilder.coloredComponent(
        boolean: Boolean,
        color: TextColor,
        vararg decoration: TextDecoration
    ) = text(boolean, color, *decoration)

    fun SurfComponentBuilder.coloredComponent(
        char: Char,
        color: TextColor,
        vararg decoration: TextDecoration
    ) = text(char, color, *decoration)

    fun SurfComponentBuilder.coloredComponent(
        number: Number,
        color: TextColor,
        vararg decoration: TextDecoration
    ) = text(number, color, *decoration)
}