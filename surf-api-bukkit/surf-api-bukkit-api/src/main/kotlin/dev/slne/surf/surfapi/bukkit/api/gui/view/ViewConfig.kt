package dev.slne.surf.surfapi.bukkit.api.gui.view

import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType

/**
 * Configuration for a GUI view.
 */
data class ViewConfig(
    var title: Component = Component.text("GUI"),
    var size: Int = 54, // 6 rows by default for CHEST
    var type: InventoryType = InventoryType.CHEST,
    var cancelOnClick: Boolean = true,
    var closeOnClickOutside: Boolean = false
) {
    /**
     * Set rows (only for CHEST type).
     * Automatically adjusts size.
     */
    var rows: Int
        get() = size / 9
        set(value) {
            require(type == InventoryType.CHEST) {
                "Rows can only be set for CHEST type inventories. For other types, the size is determined by the inventory type."
            }
            require(value in 1..6) { "Rows must be between 1 and 6" }
            size = value * 9
        }

    override fun toString(): String {
        return "ViewConfig(title=$title, size=$size, type=$type, cancelOnClick=$cancelOnClick, closeOnClickOutside=$closeOnClickOutside, rows=$rows)"
    }
}