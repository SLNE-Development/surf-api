package dev.slne.surf.surfapi.bukkit.api.gui.area

import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.ComponentPriority

/**
 * Defines the area that a component occupies in a GUI.
 * Components can have different shaped areas (cuboid, circular, custom).
 */
interface ComponentArea {
    /**
     * Priority for rendering and click handling when components overlap.
     * Higher priority components are rendered on top and handle clicks first.
     */
    val priority: ComponentPriority
    
    /**
     * Get all slots that are part of this area.
     */
    fun slots(): Set<Slot>
    
    /**
     * Check if a slot is within this area.
     */
    fun contains(slot: Slot): Boolean
    
    /**
     * Width of the bounding box containing this area.
     */
    val width: Int
    
    /**
     * Height of the bounding box containing this area.
     */
    val height: Int

    /**
     * Get the first slot in this area based on index.
     *
     * @return The slot with the lowest index, or null if area is empty.
     */
    fun first(): Slot? {
        return slots().minByOrNull { it.index }
    }

    /**
     * Get the last slot in this area based on index.
     *
     * @return The slot with the highest index, or null if area is empty.
     */
    fun last(): Slot? {
        return slots().maxByOrNull { it.index }
    }
}
