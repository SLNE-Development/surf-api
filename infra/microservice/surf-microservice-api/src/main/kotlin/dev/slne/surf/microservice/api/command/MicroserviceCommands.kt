package dev.slne.surf.microservice.api.command

import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registry of the console commands of the running microservice.
 *
 * There is one registry per JVM. Commands can be registered at any time, including in property initializers before
 * the runtime has started; the runtime reads the registry on every invocation. Thread-safe.
 */
object MicroserviceCommands {
    private val registered = CopyOnWriteArrayList<MicroserviceCommand>()

    /**
     * All registered commands in registration order.
     */
    val all: List<MicroserviceCommand> = Collections.unmodifiableList(registered)

    /**
     * Returns the command that [label] selects, or `null`.
     */
    fun find(label: String): MicroserviceCommand? {
        return registered.find { command -> command.matches(label) }
    }

    /**
     * Registers [command] and returns it.
     *
     * @throws IllegalArgumentException if its name or one of its aliases is already taken
     */
    @Synchronized
    fun register(command: MicroserviceCommand): MicroserviceCommand {
        val takenLabel = (listOf(command.name) + command.aliases)
            .find { label -> find(label) != null }
        require(takenLabel == null) { "Command label '$takenLabel' is already registered" }

        registered += command
        return command
    }

    /**
     * Registers a command that runs [block] and returns it.
     *
     * @throws IllegalArgumentException if [name] or one of [aliases] is already taken
     */
    fun register(
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
