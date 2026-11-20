package dev.slne.surf.surfapi.bukkit.test.command.dialog

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.surfapi.bukkit.test.command.dialog.subcommands.paginationDialogTestCommand
import dev.slne.surf.surfapi.bukkit.test.command.dialog.subcommands.searchDialogTestCommand
import kotlinx.coroutines.CoroutineScope

fun dialogTestCommand(pluginScope: CoroutineScope) = commandAPICommand("dialogtest") {
    paginationDialogTestCommand(pluginScope)
    searchDialogTestCommand()
}