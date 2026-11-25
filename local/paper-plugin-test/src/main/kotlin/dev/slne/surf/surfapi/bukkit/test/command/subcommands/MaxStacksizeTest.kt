package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.itemStackArgument
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsItemBridge
import dev.slne.surf.api.spring.surfBean
import org.bukkit.inventory.ItemStack

@OptIn(NmsUseWithCaution::class)
class MaxStacksizeTest(name: String) : CommandAPICommand(name) {
    init {
        itemStackArgument("item")
        integerArgument("maxStackSize", 1, 100)

        anyExecutor { sender, args ->
            val item: ItemStack by args
            val maxStackSize: Int by args
            val itemType = item.type.asItemType() ?: return@anyExecutor

            itemBridge.setDefaultMaxStackSize(itemType, maxStackSize)
            sender.sendMessage("Set max stack size of ${item.type.name} to $maxStackSize")
        }
    }
}

@OptIn(NmsUseWithCaution::class)
private val itemBridge by surfBean<SurfPaperNmsItemBridge>()
