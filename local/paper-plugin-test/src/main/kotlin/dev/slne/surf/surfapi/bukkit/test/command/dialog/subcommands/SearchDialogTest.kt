package dev.slne.surf.surfapi.bukkit.test.command.dialog.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.dialog.search.searchDialog

fun CommandAPICommand.searchDialogTestCommand() = subcommand("search") {
    greedyStringArgument("initial")

    playerExecutor { player, args ->
        val initial: String by args

        player.showDialog(
            searchDialog(
                title = {
                    text("Suche")
                },
                searchInput = {
                    initialValue = initial
                },
                body = {
                    plainMessage {
                        info("Gib einen Suchbegriff ein und klicke auf den Suchen-Button, um die Suche zu starten.")
                    }
                },
                onSearch = { p, query ->
                    p.sendText {
                        info("Du hast nach ")
                        variableValue(query)
                        info(" gesucht.")
                    }
                },
                onClose = { p, query ->
                    p.sendText {
                        info("Du hast die Suche mit dem Suchbegriff ")
                        variableValue(query)
                        info(" geschlossen.")
                    }
                }
            )
        )
    }
}