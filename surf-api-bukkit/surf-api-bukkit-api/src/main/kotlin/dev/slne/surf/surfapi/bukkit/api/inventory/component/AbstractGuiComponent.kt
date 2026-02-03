package dev.slne.surf.surfapi.bukkit.api.inventory.component

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * Abstract base class for GUI components that can be extended.
 * This provides a convenient way to create custom GUIs using class-based approach.
 */
abstract class AbstractGuiComponent(
    override val title: Component,
    override val rows: Int = 3
) : GuiComponent {
    override val parent: Component? = null
    private val _children = mutableListOf<Component>()
    override val children: List<Component> get() = _children

    override fun addChild(child: Component) {
        _children.add(child)
    }

    override fun removeChild(child: Component) {
        _children.remove(child)
    }

    override suspend fun update() {
        // Trigger update for all children
        children.forEach { child ->
            child.update()
        }
        // Override this method to add custom update logic
        onUpdate()
    }

    override suspend fun onMount() {
        // Override this method to add custom mount logic
    }

    override suspend fun onUnmount() {
        // Override this method to add custom unmount logic
    }

    override suspend fun render(player: Player) {
        // This will be implemented by the server module
    }

    /**
     * Called when the GUI is being set up.
     * Add your items and child components here.
     */
    abstract suspend fun setup()

    /**
     * Called when the GUI is updated.
     * Override this to add custom update logic.
     */
    protected open suspend fun onUpdate() {
        // Default implementation does nothing
    }

    /**
     * Adds an item component to this GUI.
     */
    protected fun addItem(item: ItemComponent) {
        addChild(item)
    }
}
