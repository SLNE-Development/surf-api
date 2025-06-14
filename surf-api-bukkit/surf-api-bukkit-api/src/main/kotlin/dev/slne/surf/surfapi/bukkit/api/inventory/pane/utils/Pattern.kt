package dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils

import dev.slne.surf.surfapi.bukkit.api.inventory.InventoryBridge

interface Pattern {
    val length: Int
    val height: Int

    fun setHeight(height: Int): Pattern
    fun setLength(length: Int): Pattern

    fun getColumn(index: Int): IntArray
    fun getRow(index: Int): IntArray
    fun contains(char: Char): Boolean
    fun getChar(x: Int, y: Int): Char

    companion object {
        operator fun invoke(vararg pattern: String): Pattern =
            InventoryBridge.instance.createPattern(*pattern)
    }
}