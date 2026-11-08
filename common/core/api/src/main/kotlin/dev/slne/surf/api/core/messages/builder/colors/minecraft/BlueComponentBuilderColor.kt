package dev.slne.surf.api.core.messages.builder.colors.minecraft

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface BlueComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.blue(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.BLUE, *decoration)

    fun SurfComponentBuilder.blue(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.BLUE, *decoration)

    fun SurfComponentBuilder.blue(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.BLUE, *decoration)

    fun SurfComponentBuilder.blue(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.BLUE, *decoration)
}