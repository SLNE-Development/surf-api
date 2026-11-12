package dev.slne.surf.microservice.runtime.command.builtin

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext

internal class ExitCommand : MicroserviceCommand("exit", "quit") {
    override val description = "Shuts down the microservice."

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        sendLine("Shutting down the microservice...")
        requestShutdown()
    }
}
