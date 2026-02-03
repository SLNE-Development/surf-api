package dev.slne.surf.surfapi.bukkit.api.gui.props

/**
 * Base interface for all props in the GUI framework.
 * All props are global to the view and shared across all viewers.
 */
sealed interface Prop<T> {
    /**
     * Gets the value of this prop.
     * Suspend function to support ComputedProp with async operations.
     */
    suspend fun get(): T
    
    /**
     * The name of this prop.
     */
    val name: String
}
