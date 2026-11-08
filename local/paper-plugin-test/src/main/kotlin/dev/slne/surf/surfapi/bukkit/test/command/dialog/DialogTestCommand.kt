package dev.slne.surf.surfapi.bukkit.test.command.dialog

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.surfapi.bukkit.test.command.dialog.subcommands.paginationDialogTestCommand
import dev.slne.surf.surfapi.bukkit.test.command.dialog.subcommands.searchDialogTestCommand

fun dialogTestCommand() = commandAPICommand("dialogtest") {
    paginationDialogTestCommand()
    searchDialogTestCommand()
}