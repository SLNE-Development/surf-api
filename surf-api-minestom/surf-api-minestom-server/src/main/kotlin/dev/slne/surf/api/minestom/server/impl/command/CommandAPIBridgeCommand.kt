package dev.slne.surf.api.minestom.server.impl.command

import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentType

/**
 * Stands in for a CommandAPI command inside Minestom's own command manager.
 *
 * Minestom offers no way to hand a whole command over to another dispatcher, so this command
 * accepts any input after its label and passes all of it on to the Brigadier dispatcher. It keeps
 * the label occupied in the manager, which makes the command reachable through
 * [net.minestom.server.command.CommandManager.execute] from players, the console and other code
 * alike, and fires [net.minestom.server.event.player.PlayerCommandEvent] as usual.
 *
 * The node Minestom declares for it is replaced by the real tree in [CommandAPIHook.declare].
 */
internal class CommandAPIBridgeCommand(names: Collection<String>) : Command(
    names.first(),
    *names.drop(1).toTypedArray(),
) {
    init {
        setDefaultExecutor(::dispatch)
        addSyntax(::dispatch, ArgumentType.StringArray(ARGUMENTS))
    }

    private fun dispatch(sender: CommandSender, context: CommandContext) {
        CommandAPIHook.execute(sender, context.input)
    }

    private companion object {
        const val ARGUMENTS = "arguments"
    }
}
