package dev.slne.surf.microservice.api.command

import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registry of the console commands of the running microservice.
 *
 * There is one registry per JVM, also reachable as
 * [Microservice.commands][dev.slne.surf.microservice.api.Microservice.commands]. Commands can be registered at any
 * time, including in property initializers of the microservice class. The runtime looks up the command on every
 * invocation, so a command registered later is available immediately. Commands cannot be unregistered. All members
 * are thread-safe.
 *
 * The runtime registers the built-in commands `exit` (alias `quit`), `health`, `help` and `spark` before it creates
 * the microservice, so these labels are always taken.
 */
public object MicroserviceCommands {
    private val registered = CopyOnWriteArrayList<MicroserviceCommand>()

    /**
     * Gets all registered commands in registration order.
     *
     * The list is a read-only live view that reflects later registrations.
     */
    public val all: List<MicroserviceCommand> = Collections.unmodifiableList(registered)

    /**
     * Returns the command that [label] selects, ignoring case, or `null` if none does.
     */
    public fun find(label: String): MicroserviceCommand? {
        return registered.find { command -> command.matches(label) }
    }

    /**
     * Registers [command] and returns it.
     *
     * @throws IllegalArgumentException if its name or one of its aliases is already taken, ignoring case
     */
    @Synchronized
    public fun register(command: MicroserviceCommand): MicroserviceCommand {
        val takenLabel = (listOf(command.name) + command.aliases)
            .find { label -> find(label) != null }
        require(takenLabel == null) { "Command label '$takenLabel' is already registered" }

        registered += command
        return command
    }

    /**
     * Creates a command that runs [block], registers it and returns it.
     *
     * Equivalent to [microserviceCommand].
     *
     * @param name the primary label of the command
     * @param aliases further labels that select the command
     * @param description the one-line description that the `help` command shows, or `null` for none
     * @param block the body of the command, called with the arguments that followed the label
     * @throws IllegalArgumentException if a label is blank or contains whitespace, or is already taken
     */
    public fun register(
        name: String,
        vararg aliases: String,
        description: String? = null,
        block: suspend MicroserviceCommandContext.(args: List<String>) -> Unit,
    ): MicroserviceCommand = register(LambdaMicroserviceCommand(name, aliases, description, block))

    private class LambdaMicroserviceCommand(
        name: String,
        aliases: Array<out String>,
        override val description: String?,
        private val block: suspend MicroserviceCommandContext.(args: List<String>) -> Unit,
    ) : MicroserviceCommand(name, *aliases) {
        override suspend fun MicroserviceCommandContext.execute(args: List<String>) = block(args)
    }
}
