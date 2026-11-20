@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.entityTypeArgument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.paper.command.args.adventureCompoundBinaryTagArgument
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsEntityBridge
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import net.kyori.adventure.nbt.CompoundBinaryTag
import org.bukkit.entity.EntityType

class SummonCommandTest(name: String) : CommandAPICommand(name) {
    init {
        entityTypeArgument("type")
        adventureCompoundBinaryTagArgument("nbt")

        playerExecutor { sender, args ->
            val type: EntityType by args
            val nbt: CompoundBinaryTag by args

            entityBridge.createEntityByNbt(sender.world, type, sender.location, nbt)
        }
    }
}

@OptIn(InternalSurfApi::class)
private val entityBridge by surfBean<SurfPaperNmsEntityBridge>()
