package dev.slne.surf.microservice.api.command

/**
 * Registers a command that runs [block] in [MicroserviceCommands] and returns it.
 *
 * @throws IllegalArgumentException if [name] or one of [aliases] is already taken
 */
fun microserviceCommand(
    name: String,
    vararg aliases: String,
    description: String? = null,
    block: suspend MicroserviceCommandContext.(args: List<String>) -> Unit,
): MicroserviceCommand = MicroserviceCommands.register(name, *aliases, description = description, block = block)
