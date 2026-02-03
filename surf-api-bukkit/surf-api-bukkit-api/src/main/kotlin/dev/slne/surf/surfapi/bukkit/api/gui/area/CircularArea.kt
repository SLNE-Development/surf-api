package dev.slne.surf.surfapi.bukkit.api.gui.area

import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * A circular area defined by a center point and radius.
 * Useful for radial menus, circular patterns, or highlighting areas.
 */
data class CircularArea(
    val center: Slot,
    val radius: Double
) : ComponentArea {
    override fun slots(): Set<Slot> {
        val slots = mutableSetOf<Slot>()
        val radiusCeil = ceil(radius).toInt()
        
        // Check all slots within the bounding box
        for (row in (center.row - radiusCeil)..(center.row + radiusCeil)) {
            for (col in (center.column - radiusCeil)..(center.column + radiusCeil)) {
                val slot = Slot.at(col, row)
                if (contains(slot)) {
                    slots.add(slot)
                }
            }
        }
        
        return slots
    }
    
    override fun contains(slot: Slot): Boolean {
        val dx = (slot.column - center.column).toDouble()
        val dy = (slot.row - center.row).toDouble()
        val distance = sqrt(dx * dx + dy * dy)
        return distance <= radius
    }
    
    override val width: Int
        get() = (ceil(radius) * 2 + 1).toInt()
    
    override val height: Int
        get() = (ceil(radius) * 2 + 1).toInt()
}
