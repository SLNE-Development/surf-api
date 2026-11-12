package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.SurfAsyncEventHandlerTest
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.SurfSyncEventHandlerTest

class SurfEventHandlerTest(name: String) : CommandAPICommand(name) {
    init {
        withSubcommands(
            SurfSyncEventHandlerTest("sync"),
            SurfAsyncEventHandlerTest("async")
        )
    }
}