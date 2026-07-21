package dev.slne.surf.microservice.api.microservice.command

import dev.slne.surf.api.core.util.requiredService

private val commandManager = requiredService<MicroserviceCommandManager>()

interface MicroserviceCommandManager {
    val commands: List<MicroserviceCommand>

    fun findCommand(label: String): MicroserviceCommand?

    fun registerCommand(command: MicroserviceCommand)

    suspend fun executeCommand(
        input: String,
        context: MicroserviceCommandContext
    ): MicroserviceCommandResult

    suspend fun executeCommand(
        label: String,
        args: List<String>,
        context: MicroserviceCommandContext
    ): MicroserviceCommandResult

    companion object : MicroserviceCommandManager by commandManager {
        val INSTANCE get() = commandManager
    }
}