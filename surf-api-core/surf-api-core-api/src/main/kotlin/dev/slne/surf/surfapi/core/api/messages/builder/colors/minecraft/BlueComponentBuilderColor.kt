package dev.slne.surf.surfapi.core.api.messages.builder.colors.minecraft

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.ComponentBuilderColor
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
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