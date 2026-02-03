package dev.slne.surf.surfapi.bukkit.api.inventory.component

import org.bukkit.entity.Player

/**
 * Base interface for all components in the inventory framework.
 * Components follow a React-like pattern where they can have children and trigger updates.
 */
interface Component {
    /**
     * The parent component, or null if this is a root component.
     */
    val parent: Component?

    /**
     * List of child components.
     */
    val children: List<Component>

    /**
     * Adds a child component to this component.
     */
    fun addChild(child: Component)

    /**
     * Removes a child component from this component.
     */
    fun removeChild(child: Component)

    /**
     * Called when the component should update.
     * This will trigger updates for all child components.
     */
    suspend fun update()

    /**
     * Called when the component is mounted (added to the component tree).
     */
    suspend fun onMount()

    /**
     * Called when the component is unmounted (removed from the component tree).
     */
    suspend fun onUnmount()

    /**
     * Checks if this component should be rendered based on its current state.
     */
    fun shouldRender(): Boolean = true
}

/**
 * A component that can be rendered to a player.
 */
interface RenderableComponent : Component {
    /**
     * Renders this component for the given player.
     */
    suspend fun render(player: Player)
}
