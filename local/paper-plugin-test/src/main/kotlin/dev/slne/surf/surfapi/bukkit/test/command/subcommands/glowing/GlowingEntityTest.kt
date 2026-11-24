package dev.slne.surf.surfapi.bukkit.test.command.subcommands.glowing

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.booleanArgument
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOneEntity
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.api.spring.surfBean
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Entity

class GlowingEntityTest(name: String) : CommandAPICommand(name) {

    init {
        booleanArgument("glow")
        entitySelectorArgumentOneEntity("entity")
        playerExecutor { sender, args ->
            val glow: Boolean by args
            val entity: Entity by args

            if (glow) {
                glowingApi.makeGlowing(entity, sender, NamedTextColor.RED)
            } else {
                glowingApi.removeGlowing(entity, sender)
            }

            val message = if (glow) {
                "You made ${entity.name} glowing!"
            } else {
                "You removed glowing from ${entity.name}!"
            }
            sender.sendMessage(message)
        }
    }
}

private val glowingApi by surfBean<SurfGlowingApi>()
