package dev.slne.surf.surfapi.bukkit.api.inventory.utils

import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

internal class PlayerInventoryCache {

    private val inventories = mutableObject2ObjectMapOf<Player, Array<ItemStack?>>()

    fun storeAndClear(player: Player) {
        store(player)

        val inventory = player.inventory
        for (i in 0 until inventory.size) {
            inventory.clear(i)
        }
    }

    fun restoreAndForget(player: Player) {
        restore(player)
        clearCache(player)
    }

    fun restoreAndForgetAll() {
        restoreAll()
        clearCache()
    }

    fun add(player: Player, item: ItemStack): Int {
        val items = inventories[player]
            ?: throw IllegalStateException("The player ${player.uniqueId} does not have a cached inventory")

        var amountPutIn = 0

        for (i in 0 until items.size) {
            val itemStack = items[i]

            if (itemStack == null) {
                items[i] = item.clone()
                items[i]!!.amount = item.amount - amountPutIn
                amountPutIn = item.amount
                break
            }

            if (!itemStack.isSimilar(item)) {
                continue
            }

            val additionalAmount = minOf(itemStack.maxStackSize - itemStack.amount, item.amount)
            itemStack.amount = itemStack.amount + additionalAmount
            amountPutIn += additionalAmount

            if (amountPutIn == item.amount) {
                break
            }
        }

        return item.amount - amountPutIn
    }

    fun store(player: Player) {
        val inventory = player.inventory
        val items = Array<ItemStack?>(inventory.size) { null }

        for (i in 0 until inventory.size) {
            items[i] = inventory.getItem(i)
        }

        inventories[player] = items
    }

    private fun restore(player: Player) {
        val items = inventories[player] ?: return
        val inventory = player.inventory

        for (i in 0 until items.size) {
            inventory.setItem(i, items[i])
        }
    }

    private fun restoreAll() {
        inventories.keys.forEach(this::restore)
    }

    fun contains(player: Player) = inventories.containsKey(player)

    fun clearCache(player: Player) {
        inventories.remove(player)
    }

    fun clearCache() {
        inventories.clear()
    }

}