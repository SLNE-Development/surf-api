package dev.slne.surf.surfapi.bukkit.api.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Represents an item in a GUI with utility functions.
 * Wraps an ItemStack and provides additional functionality.
 */
class GuiItem(
    val itemStack: ItemStack
) {
    
    /**
     * Get the material of this item.
     */
    val material: Material
        get() = itemStack.type
    
    /**
     * Get the amount of this item.
     */
    val amount: Int
        get() = itemStack.amount
    
    /**
     * Check if this item is empty (AIR).
     */
    val isEmpty: Boolean
        get() = itemStack.type == Material.AIR || itemStack.amount == 0
    
    /**
     * Create a copy of this GuiItem.
     */
    fun copy(): GuiItem = GuiItem(itemStack.clone())
    
    /**
     * Create a copy with modified properties.
     */
    fun copyWith(
        material: Material? = null,
        amount: Int? = null
    ): GuiItem {
        val newStack = itemStack.clone()
        material?.let { newStack.type = it }
        amount?.let { newStack.amount = it }
        return GuiItem(newStack)
    }
    
    companion object {
        /**
         * Create a GuiItem from an ItemStack.
         */
        fun of(itemStack: ItemStack): GuiItem = GuiItem(itemStack)
        
        /**
         * Create a GuiItem from a material.
         */
        fun of(material: Material, amount: Int = 1): GuiItem {
            return GuiItem(ItemStack(material, amount))
        }
        
        /**
         * Create an empty GuiItem (AIR).
         */
        fun empty(): GuiItem = GuiItem(ItemStack(Material.AIR))
    }
}

/**
 * Extension function to convert ItemStack to GuiItem.
 */
fun ItemStack.toGuiItem(): GuiItem = GuiItem.of(this)

/**
 * Extension function to convert GuiItem to ItemStack.
 */
fun GuiItem.toItemStack(): ItemStack = this.itemStack
