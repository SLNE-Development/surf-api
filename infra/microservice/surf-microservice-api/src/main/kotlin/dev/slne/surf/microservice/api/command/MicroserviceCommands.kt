package dev.slne.surf.microservice.api.command

import dev.slne.surf.microservice.api.InternalMicroserviceApi
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registry of the console commands of the running microservice.
 *
 * There is one registry per JVM. Every `MicroserviceCommand` bean of the application is registered while the
 * context starts; commands can also be registered directly, at any time. The runtime looks up the command on every
 * invocation, so a command registered later is available immediately. Commands cannot be unregistered. All members
 * are thread-safe.
 *
 * The runtime registers the built-in commands `exit` (alias `quit`), `health`, `help` and `spark` once all singletons
 * of the context exist, before the `MicroserviceCommand` beans. A built-in is skipped, with a warning, if one of its
 * labels is taken already, so a command registered earlier, for example in `main`, replaces it; a replacement of
 * `exit` has to request the shutdown itself. A `MicroserviceCommand` bean that reuses the label of a registered
 * built-in fails the start.
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
        val takenLabel = firstTakenLabel(command)
        require(takenLabel == null) { "Command label '$takenLabel' is already registered" }

        registered += command
        return command
    }

    /**
     * Registers [command] unless its name or one of its aliases is taken already.
     *
     * Checks and registers under the lock of [register], so concurrent callers cannot both claim a label. Unlike
     * [register], it does not throw for a taken label.
     *
     * @return `null` if [command] was registered, otherwise the first of its labels that is taken
     */
    @InternalMicroserviceApi
    @Synchronized
    public fun registerIfAbsent(command: MicroserviceCommand): String? {
        val takenLabel = firstTakenLabel(command)
        if (takenLabel == null) registered += command
        return takenLabel
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

    private fun firstTakenLabel(command: MicroserviceCommand): String? =
        (listOf(command.name) + command.aliases).find { label -> find(label) != null }

    private class LambdaMicroserviceCommand(
        name: String,
        aliases: Array<out String>,
        override val description: String?,
        private val block: suspend MicroserviceCommandContext.(args: List<String>) -> Unit,
    ) : MicroserviceCommand(name, *aliases) {
        override suspend fun MicroserviceCommandContext.execute(args: List<String>) = block(args)
    }
}
