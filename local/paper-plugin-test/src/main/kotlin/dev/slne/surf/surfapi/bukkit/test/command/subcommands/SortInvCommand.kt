package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsItemBridge
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean

@OptIn(NmsUseWithCaution::class)
class SortInvCommand(name: String) : CommandAPICommand(name) {

    init {
        playerExecutor { player, _ ->
            val inv = player.inventory
            val sorted = inv.contents.apply { sortWith(itemBridge.getCreativeSearchItemOrderComparator()) }
            inv.contents = sorted
        }
    }
}

@OptIn(InternalSurfApi::class)
private val itemBridge by surfBean<SurfPaperNmsItemBridge>()
