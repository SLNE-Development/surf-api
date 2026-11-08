package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsItemBridge

@OptIn(NmsUseWithCaution::class)
class SortInvCommand(name: String) : CommandAPICommand(name) {

    init {
        playerExecutor { player, _ ->
            val inv = player.inventory
            val sorted = inv.contents.apply { sortWith(SurfPaperNmsItemBridge.getCreativeSearchItemOrderComparator()) }
            inv.contents = sorted
        }
    }
}