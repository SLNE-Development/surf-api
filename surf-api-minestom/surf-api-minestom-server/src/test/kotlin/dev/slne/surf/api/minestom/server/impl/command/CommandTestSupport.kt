package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.argument.ArgumentDefinition
import dev.slne.surf.api.minestom.server.impl.command.brigadier.NodeDeclaration
import dev.slne.surf.api.minestom.server.impl.command.brigadier.NodeDeclarations
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket
import net.minestom.testing.Env
import dev.slne.surf.api.minestom.permission.MinestomPermissions
import dev.slne.surf.api.minestom.permission.PermissionProvider
import net.kyori.adventure.util.TriState
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection

/**
 * Runs [input] as [sender] and reports whether a command executed.
 *
 * A rejected command — unknown, badly formed, or hidden from this sender because it lacks the
 * permission — returns `false` rather than throwing, so a test can assert either outcome. The
 * rejection is not reported to [sender]; use [runCommandReporting] to observe the message a real
 * command packet would produce.
 */
internal fun runCommand(sender: CommandSender, input: String): Boolean = try {
    CommandAPI.execute(sender, input)
    true
} catch (failure: CommandSyntaxException) {
    false
}

/**
 * Runs [input] as [sender] the way the command manager does, reporting a rejection to the sender,
 * and reports whether a command executed.
 */
internal fun runCommandReporting(sender: CommandSender, input: String): Boolean =
    CommandAPIHook.execute(sender, input)?.type == CommandResult.Type.SUCCESS

/** Runs [input] as [sender] and returns the executed command's result value. */
internal fun runCommandForResult(sender: CommandSender, input: String): Int =
    CommandAPI.execute(sender, input)

/** Reads [input] with this argument's own parser, the way the dispatcher does. */
internal fun <T> ArgumentDefinition<T>.read(sender: CommandSender, input: String): T =
    rawType.parse(StringReader(input), sender)

/** How this argument is announced to a client. */
internal fun ArgumentDefinition<*>.declaration(): NodeDeclaration = NodeDeclarations().of(this)

/**
 * The command tree [player] would be sent: Minestom's own commands with the CommandAPI's merged in.
 */
internal fun declaredCommands(env: Env, player: Player): DeclareCommandsPacket {
    val original = env.process().command().createDeclareCommandsPacket(player)
    val merger = MinestomCommandAPIPlatform.activeMerger()
        ?: error("No Minestom CommandAPI platform is installed")
    return merger.merge(original, player)
}

/**
 * A player holding exactly [grantedPermissions], resolved through [MinestomPermissions].
 */
internal class TestPermissionPlayer(
    connection: PlayerConnection,
    profile: GameProfile,
    val grantedPermissions: Set<String>,
) : Player(connection, profile) {
    init {
        MinestomPermissions.installProvider(TEST_PERMISSIONS)
    }
}

private val TEST_PERMISSIONS = PermissionProvider { player, permission ->
    TriState.byBoolean((player as? TestPermissionPlayer)?.grantedPermissions?.contains(permission) == true)
}
