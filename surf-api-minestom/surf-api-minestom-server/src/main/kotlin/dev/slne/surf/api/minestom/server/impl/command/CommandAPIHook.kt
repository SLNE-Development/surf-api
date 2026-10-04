package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.executor.GENERIC_COMMAND_FAILURE
import dev.slne.surf.api.minestom.server.impl.command.brigadier.reportSyntaxFailure
import dev.slne.surf.api.minestom.server.impl.command.brigadier.reportUnknownCommand
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket

object CommandAPIHook {

    /** Whether [name] is a command label the CommandAPI owns. */
    @JvmStatic
    fun owns(name: String): Boolean = ownership()?.contains(name) == true

    /**
     * Runs [input] through Brigadier when its command label belongs to the CommandAPI, returning
     * `null` when it does not so the manager keeps parsing it itself.
     *
     * A rejection is reported to [sender] the way vanilla reports it.
     */
    @JvmStatic
    fun execute(sender: CommandSender, input: String): CommandResult? {
        val ownership = ownership() ?: return null
        if (ownership.findInput(input) == null) return null

        val command = input.removePrefix("/")

        return try {
            CommandAPI.execute(sender, command)
            CommandResult.of(CommandResult.Type.SUCCESS, command)
        } catch (failure: CommandSyntaxException) {
            reportSyntaxFailure(sender, command, failure)
            CommandResult.of(CommandResult.Type.INVALID_SYNTAX, command)
        } catch (failure: Throwable) {
            MinecraftServer.LOGGER.error("Command '{}' failed for {}", command, sender, failure)
            sender.sendMessage(GENERIC_COMMAND_FAILURE)
            CommandResult.of(CommandResult.Type.CANCELLED, command)
        }
    }

    /** [packet] with the CommandAPI's commands merged into the tree [player] is shown. */
    @JvmStatic
    fun declare(packet: DeclareCommandsPacket, player: Player): DeclareCommandsPacket =
        MinestomCommandAPIPlatform.activeMerger()?.merge(packet, player) ?: packet

    /**
     * Reports an input no registry could resolve, in vanilla's layout.
     *
     * Installed as the manager's unknown command callback, which Minestom leaves unset.
     */
    @JvmStatic
    fun reportUnknown(sender: CommandSender, input: String) =
        reportUnknownCommand(sender, input.removePrefix("/"))

    private fun ownership(): MinestomCommandOwnership? =
        MinestomCommandAPIPlatform.activeOwnership()
}
