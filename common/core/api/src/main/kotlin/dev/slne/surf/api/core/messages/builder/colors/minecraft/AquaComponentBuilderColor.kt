package dev.slne.surf.api.core.messages.builder.colors.minecraft

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface AquaComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.aqua(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.AQUA, *decoration)

    fun SurfComponentBuilder.aqua(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.AQUA, *decoration)

    fun SurfComponentBuilder.aqua(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.AQUA, *decoration)

    fun SurfComponentBuilder.aqua(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.AQUA, *decoration)
}