package dev.slne.surf.surfapi.core.api.extensions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ScopedComponent

operator fun Component.plus(other: Component) = append(other)
operator fun Component.plus(other: String) = append(Component.text(other))
operator fun <C : Component> ScopedComponent<C>.plus(other: Component) = append(other)
operator fun <C : Component> ScopedComponent<C>.plus(other: String) = append(Component.text(other))