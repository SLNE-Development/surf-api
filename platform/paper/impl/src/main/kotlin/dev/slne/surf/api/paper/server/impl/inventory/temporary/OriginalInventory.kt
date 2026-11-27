package dev.slne.surf.api.paper.server.impl.inventory.temporary

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/**
 * A copy of every slot of a player inventory and its selected hotbar slot, taken when a session
 * begins.
 */
internal class OriginalInventory private constructor(
    private val items: List<ItemStack?>,
    val selectedSlot: Int,
) {

    /**
     * Serializes every slot with Paper's item serialization; an empty slot becomes an empty array.
     */
    fun serialize(): SerializedSlots {
        val slots = ArrayList<ByteArray>(items.size)

        for ((slot, item) in items.withIndex()) {
            if (item == null) {
                slots += ByteArray(0)
                continue
            }

            try {
                slots += item.serializeAsBytes()
            } catch (failure: Exception) {
                return SerializedSlots.Failed(slot, failure)
            }
        }

        return SerializedSlots.Stored(slots)
    }

    /**
     * Puts every slot and the selected hotbar slot back into [inventory].
     */
    fun restore(inventory: PlayerInventory) {
        for ((slot, item) in items.withIndex()) {
            inventory.setItem(slot, item)
        }

        inventory.heldItemSlot = selectedSlot
    }

    companion object {

        fun capture(inventory: PlayerInventory): OriginalInventory {
            val items = List(inventory.size) { slot ->
                inventory.getItem(slot)
                    ?.takeUnless { it.isEmpty }
                    ?.clone()
            }

            return OriginalInventory(items, inventory.heldItemSlot)
        }
    }
}

internal sealed interface SerializedSlots {

    class Stored(val slots: List<ByteArray>) : SerializedSlots

    class Failed(val slot: Int, val cause: Exception) : SerializedSlots
}
