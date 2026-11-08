package dev.slne.surf.api.core.messages.builder.colors

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.ComponentBuilderColor
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextDecoration

interface VariableValueComponentBuilderColor : ComponentBuilderColor {
    fun SurfComponentBuilder.variableValue(text: String, vararg decoration: TextDecoration) =
        coloredComponent(text, Colors.VARIABLE_VALUE, *decoration)

    fun SurfComponentBuilder.variableValue(boolean: Boolean, vararg decoration: TextDecoration) =
        coloredComponent(boolean, Colors.VARIABLE_VALUE, *decoration)

    fun SurfComponentBuilder.variableValue(char: Char, vararg decoration: TextDecoration) =
        coloredComponent(char, Colors.VARIABLE_VALUE, *decoration)

    fun SurfComponentBuilder.variableValue(number: Number, vararg decoration: TextDecoration) =
        coloredComponent(number, Colors.VARIABLE_VALUE, *decoration)
}