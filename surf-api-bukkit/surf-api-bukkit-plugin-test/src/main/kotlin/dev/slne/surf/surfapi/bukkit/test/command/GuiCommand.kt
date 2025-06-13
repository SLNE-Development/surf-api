package dev.slne.surf.surfapi.bukkit.test.command

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.surfapi.bukkit.test.gui.exampleGui

fun guiCommand() = commandAPICommand("gui") {
    playerExecutor { player, args ->
        exampleGui().show(player)
    }
}