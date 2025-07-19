package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer.AreaLocationVisualizerTest
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer.SingleLocationVisualizerTest

class VisualizerTest(name: String) : CommandAPICommand(name) {
    init {
        withSubcommands(
            SingleLocationVisualizerTest("singleLocation"),
            AreaLocationVisualizerTest("areaLocation"),
        )
    }
}