package dev.slne.surf.api.core.messages.builder.colors

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface VariableKeyComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.variableKey(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.VARIABLE_KEY, *decoration)

    fun SurfComponentBuilder.variableKey(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.VARIABLE_KEY, *decoration)

    fun SurfComponentBuilder.variableKey(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.VARIABLE_KEY, *decoration)

    fun SurfComponentBuilder.variableKey(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.VARIABLE_KEY, *decoration)
}