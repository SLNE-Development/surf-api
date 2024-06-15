package dev.slne.surf.surfapi.core.api.extensions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ScopedComponent
import net.kyori.adventure.text.TextComponent

operator fun Component.plus(other: Component) = append(other)
operator fun Component.plus(other: String) = append(Component.text(other))
operator fun <C : Component> ScopedComponent<C>.plus(other: Component) = append(other)
operator fun <C : Component> ScopedComponent<C>.plus(other: String) = append(Component.text(other))

fun Component.repeat(times: Int): Component {
    check(times > 0) { "times must be > 0" }
    
    var component = this
    repeat(times - 1) {
        component += component
    }

    return component
}

fun <C : Component> ScopedComponent<C>.repeat(times: Int): C {
    check(times > 0) { "times must be > 0" }

    var component = this + Component.empty()
    repeat(times - 1) {
        component = (component + component) as C
    }

    return component
}

@Target(AnnotationTarget.TYPE)
@DslMarker
annotation class ComponentBuilderMarker

fun buildText(init: (@ComponentBuilderMarker TextComponent.Builder).() -> Unit): TextComponent {
    val builder = Component.text()
    builder.init()
    return builder.build()
}