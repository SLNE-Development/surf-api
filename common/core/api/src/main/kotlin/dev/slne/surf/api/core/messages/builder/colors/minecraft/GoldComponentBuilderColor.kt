package dev.slne.surf.api.core.messages.builder.colors.minecraft

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface GoldComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.gold(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.GOLD, *decoration)

    fun SurfComponentBuilder.gold(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.GOLD, *decoration)

    fun SurfComponentBuilder.gold(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.GOLD, *decoration)

    fun SurfComponentBuilder.gold(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.GOLD, *decoration)
}