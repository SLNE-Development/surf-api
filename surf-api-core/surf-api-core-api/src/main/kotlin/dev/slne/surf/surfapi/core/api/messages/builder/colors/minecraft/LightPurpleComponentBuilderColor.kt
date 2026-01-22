package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface LightPurpleComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.lightPurple(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.LIGHT_PURPLE, *decoration)

    fun SurfComponentBuilder.lightPurple(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.LIGHT_PURPLE, *decoration)

    fun SurfComponentBuilder.lightPurple(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.LIGHT_PURPLE, *decoration)

    fun SurfComponentBuilder.lightPurple(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.LIGHT_PURPLE, *decoration)
}