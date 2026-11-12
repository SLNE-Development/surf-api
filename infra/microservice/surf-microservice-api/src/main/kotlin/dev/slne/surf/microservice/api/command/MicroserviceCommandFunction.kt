package dev.slne.surf.microservice.api.command

/**
 * Creates a command that runs [block], registers it in [MicroserviceCommands] and returns it.
 *
 * ```kotlin
 * microserviceCommand("echo", "say", description = "Repeats its arguments.") { args ->
 *     if (args.isEmpty()) fail("Usage: echo <text>")
 *     sendLine(args.joinToString(" "))
 * }
 * ```
 *
 * @param name the primary label of the command
 * @param aliases further labels that select the command
 * @param description the one-line description that the `help` command shows, or `null` for none
 * @param block the body of the command, called with the arguments that followed the label
 * @throws IllegalArgumentException if a label is blank or contains whitespace, or is already taken
 */
public fun microserviceCommand(
    name: String,
    vararg aliases: String,
    description: String? = null,
    block: suspend MicroserviceCommandContext.(args: List<String>) -> Unit,
): MicroserviceCommand = MicroserviceCommands.register(name, *aliases, description = description, block = block)
