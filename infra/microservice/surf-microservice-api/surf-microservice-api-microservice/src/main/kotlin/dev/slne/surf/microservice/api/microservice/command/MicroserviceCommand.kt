package dev.slne.surf.microservice.api.microservice.command

fun microserviceCommand(
    name: String,
    vararg aliases: String,
    block: suspend MicroserviceCommandContext.(args: List<String>) -> Unit
): MicroserviceCommand = object : MicroserviceCommand(name, *aliases) {
    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        block(args)
    }
}.register()

abstract class MicroserviceCommand(
    val name: String,
    vararg val aliases: String
) {
    abstract suspend fun MicroserviceCommandContext.execute(args: List<String>)

    fun register(): MicroserviceCommand {
        MicroserviceCommandManager.registerCommand(this)

        return this
    }
}