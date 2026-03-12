package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.util.shift
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import java.util.concurrent.CopyOnWriteArrayList

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