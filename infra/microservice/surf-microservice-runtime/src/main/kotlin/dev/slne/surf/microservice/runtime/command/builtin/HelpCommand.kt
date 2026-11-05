package dev.slne.surf.microservice.runtime.command.builtin

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.microservice.api.command.MicroserviceCommands

/**
 * Lists every registered command, one line each, sorted by name: `name (alias, …) - description`.
 */
internal class HelpCommand(
    private val commands: () -> List<MicroserviceCommand> = { MicroserviceCommands.all },
) : MicroserviceCommand("help") {
    override val description = "Lists all commands."

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        sendLine("Commands:")
        commands().sortedBy { command -> command.name.lowercase() }.forEach { command -> sendLine("  ${describe(command)}") }
    }

    private fun describe(command: MicroserviceCommand) = buildString {
        append(command.name)
        if (command.aliases.isNotEmpty()) append(command.aliases.joinToString(prefix = " (", postfix = ")"))
        command.description?.let { description -> append(" - ").append(description) }
    }
}
