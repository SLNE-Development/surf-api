package dev.slne.surf.api.paper.inventory.framework.view.container

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.api.paper.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.api.paper.inventory.framework.view.util.shift
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An ordered, thread-safe collection of [ViewContainerComponent]s that compose the
 * rendered inventory title.
 *
 * Components are stored in a [CopyOnWriteArrayList] to allow concurrent iteration and
 * modification. Duplicate components (by [equals]/[hashCode]) are silently ignored on
 * [addChild]. The [render] method builds the final Adventure [Component][net.kyori.adventure.text.Component]
 * by iterating each child and applying its positional shift glyphs around its visual.
 *
 * This class is `@PublishedApi internal` — it is not part of the public API. Use
 * [ViewContainerModificationContext] and the DSL helpers in `ViewContainerDSL.kt` instead.
 *
 * @see ViewContainerComponent
 * @see ViewContainerModificationContext
 */
@PublishedApi
internal class ViewContainer {
    private val _children = CopyOnWriteArrayList<ViewContainerComponent>()
    val children: List<ViewContainerComponent> get() = _children

    fun addChild(component: ViewContainerComponent) {
        _children.addIfAbsent(component)
    }

    fun removeChild(component: ViewContainerComponent) {
        _children.remove(component)
    }

    fun <T : ViewContainerComponent> hasComponentOfType(type: Class<T>): Boolean {
        return children.any { type.isInstance(it) }
    }

    inline fun <reified T : ViewContainerComponent> hasComponentOfType(): Boolean {
        return hasComponentOfType(T::class.java)
    }

    fun <T : ViewContainerComponent> removeChildrenOfType(type: Class<T>) {
        _children.removeIf { type.isInstance(it) }
    }

    inline fun <reified T : ViewContainerComponent> removeChildrenOfType() {
        removeChildrenOfType(T::class.java)
    }

    fun render() = buildText {
        for (component in children) {
            append(component.render())
        }

        font(SurfViewSettingsDefaults.DEFAULT_MENU_FONT)
    }

    private fun ViewContainerComponent.render() = buildText {
        if (positionalShift != 0) {
            append { text(shift(positionalShift)) }
        }

        append { renderComponent() }

        val resetShift = -(textureWidth + positionalShift)
        if (resetShift != 0) {
            append { text(shift(resetShift)) }
        }
    }
}