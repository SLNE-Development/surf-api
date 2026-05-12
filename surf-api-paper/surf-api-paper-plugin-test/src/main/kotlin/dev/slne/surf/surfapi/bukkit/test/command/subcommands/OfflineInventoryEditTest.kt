@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import com.destroystokyo.paper.profile.PlayerProfile
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.AsyncPlayerProfileArgument
import dev.jorel.commandapi.kotlindsl.argument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.api.paper.command.util.awaitAsyncPlayerProfile
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import dev.slne.surf.surfapi.bukkit.test.command.args.EquipmentSlotArgument
import org.bukkit.command.CommandSender
import org.bukkit.inventory.EquipmentSlot

class OfflineInventoryEditTest(name: String) : CommandAPICommand(name) {
    init {
        setSlotCommand()
        setEquipmentCommand()
        summaryCommand()
        clearCommand()
    }

    private fun setSlotCommand() = subcommand("set-slot") {
        argument(AsyncPlayerProfileArgument("player"))
        integerArgument("slot", 0, 35)

        playerExecutorSuspend { sender, args ->
            val target = args.awaitAsyncPlayerProfile("player")
            val slot: Int by args
            val item = sender.inventory.itemInMainHand.clone()

            if (item.isEmpty) {
                sender.sendMessage("Hold an item in your main hand to copy it into the offline inventory.")
                return@playerExecutorSuspend
            }

            if (!runEdit(sender, target) { edit ->
                edit.items[slot] = item
            }) return@playerExecutorSuspend

            sender.sendMessage("Set slot $slot of ${target.name} to ${item.type.key.asString()} x${item.amount}.")
        }
    }

    private fun setEquipmentCommand() = subcommand("set-equipment") {
        argument(AsyncPlayerProfileArgument("player"))
        argument(EquipmentSlotArgument("slot"))

        playerExecutorSuspend { sender, args ->
            val target = args.awaitAsyncPlayerProfile("player")
            val slot: EquipmentSlot by args
            val item = sender.inventory.itemInMainHand.clone()

            if (item.isEmpty) {
                sender.sendMessage("Hold an item in your main hand to copy it into the offline equipment.")
                return@playerExecutorSuspend
            }

            if (!runEdit(sender, target) { edit ->
                edit.equipment.setItem(slot, item)
            }) return@playerExecutorSuspend

            sender.sendMessage("Set equipment slot ${slot.name} of ${target.name} to ${item.type.key.asString()} x${item.amount}.")
        }
    }

    private fun summaryCommand() = subcommand("summary") {
        argument(AsyncPlayerProfileArgument("player"))

        anyExecutorSuspend { sender, args ->
            val target = args.awaitAsyncPlayerProfile("player")
            var inventoryItems = emptyList<String>()
            var equipmentItems = emptyList<String>()

            if (!runEdit(sender, target) { edit ->
                inventoryItems = edit.items.withIndex()
                    .filter { (_, item) -> !item.isEmpty }
                    .map { (slot, item) -> "slot $slot=${item.type.key.asString()} x${item.amount}" }

                equipmentItems = EDITABLE_EQUIPMENT_SLOTS
                    .map { slot -> slot to edit.equipment.getItem(slot) }
                    .filter { (_, item) -> !item.isEmpty }
                    .map { (slot, item) -> "${slot.name}=${item.type.key.asString()} x${item.amount}" }
            }) return@anyExecutorSuspend

            sender.sendMessage("Offline inventory summary for ${target.name}:")
            sender.sendMessage("Inventory: ${inventoryItems.ifEmpty { listOf("empty") }.joinToString()}")
            sender.sendMessage("Equipment: ${equipmentItems.ifEmpty { listOf("empty") }.joinToString()}")
        }
    }

    private fun clearCommand() = subcommand("clear") {
        argument(AsyncPlayerProfileArgument("player"))

        anyExecutorSuspend { sender, args ->
            val target = args.awaitAsyncPlayerProfile("player")

            if (!runEdit(sender, target) { edit ->
                edit.items.clear()
                edit.equipment.clear()
            }) return@anyExecutorSuspend

            sender.sendMessage("Cleared offline inventory and equipment of ${target.name}.")
        }
    }

    private suspend fun runEdit(
        sender: CommandSender,
        target: PlayerProfile,
        edit: (SurfPaperNmsPlayerBridge.PlayerInventoryEdit) -> Unit
    ): Boolean {
        return runCatching {
            SurfPaperNmsPlayerBridge.editOfflineInventory(target, edit)
        }.onFailure { error ->
            sender.sendMessage("editOfflineInventory failed: ${error::class.simpleName}: ${error.message}")
        }.isSuccess
    }

    companion object {
        private val EDITABLE_EQUIPMENT_SLOTS = listOf(
            EquipmentSlot.HAND,
            EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        )
    }
}
