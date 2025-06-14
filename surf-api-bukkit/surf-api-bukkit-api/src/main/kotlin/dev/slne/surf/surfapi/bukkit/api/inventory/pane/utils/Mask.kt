package dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils

import dev.slne.surf.surfapi.bukkit.api.inventory.InventoryBridge

interface Mask {
    val enabledSlots: Int
    val length: Int
    val height: Int

    fun setHeight(height: Int): Mask
    fun setLength(length: Int): Mask
    fun getColumn(index: Int): BooleanArray
    fun getRow(index: Int): BooleanArray
    fun isEnabled(x: Int, y: Int): Boolean

    companion object {
        operator fun invoke(vararg mask: String): Mask = InventoryBridge.instance.createMask(*mask)
    }
}