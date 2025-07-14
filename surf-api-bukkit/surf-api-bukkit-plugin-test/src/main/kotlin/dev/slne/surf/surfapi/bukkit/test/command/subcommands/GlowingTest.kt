package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.glowing.GlowingEntityTest

class GlowingTest(name: String): CommandAPICommand(name) {
    init {
        withSubcommands(GlowingEntityTest("entity"))
    }
}