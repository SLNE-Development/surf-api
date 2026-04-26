package dev.slne.surf.surfapi.bukkit.test.command.subcommands.glowing

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ChatColorArgument
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.booleanArgument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.paper.glow.SurfGlowingApi
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location

class GlowingBlockTest(name: String) : CommandAPICommand(name) {
    init {
        booleanArgument("glow")
        locationArgument("location", LocationType.BLOCK_POSITION)
        ChatColorArgument("color")

        playerExecutor { sender, args ->
            val glow: Boolean by args
            val location: Location by args
            val color: NamedTextColor by args

            if (glow) {
                SurfGlowingApi.makeGlowing(location, sender, color)
            } else {
                SurfGlowingApi.removeGlowing(location, sender)
            }

            sender.sendMessage("Block at ${location.blockX}, ${location.blockY}, ${location.blockZ} is now ${if (glow) "glowing" else "not glowing"} with color $color.")
        }

    }
}