package dev.slne.surf.surfapi.bukkit.api.gui.ref

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import java.util.concurrent.atomic.AtomicReference

/**
 * React-like ref system for component interaction.
 * Allows components to reference and update other components.
 */
class Ref<T : Component> {
    private val reference = AtomicReference<T?>()
    
    /**
     * Gets the current component reference.
     */
    val current: T?
        get() = reference.get()
    
    /**
     * Sets the component reference.
     */
    fun set(component: T?) {
        reference.set(component)
    }
    
    /**
     * Updates the referenced component.
     * @param viewer The specific viewer to update for, or null to update for all viewers
     */
    fun update(viewer: Player? = null) {
        current?.update(viewer)
    }
    
    /**
     * Checks if the ref has a current value.
     */
    fun isSet(): Boolean = current != null
}

/**
 * Creates a new ref for a component.
 */
fun <T : Component> createRef(): Ref<T> = Ref()

/**
 * DSL marker for ref operations.
 */
@DslMarker
annotation class RefMarker
