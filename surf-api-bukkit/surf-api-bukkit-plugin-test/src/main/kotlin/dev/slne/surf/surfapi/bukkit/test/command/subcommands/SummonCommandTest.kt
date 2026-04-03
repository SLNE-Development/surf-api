@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.entityTypeArgument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.surfapi.bukkit.api.command.args.adventureCompoundBinaryTagArgument
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.entityBridge
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