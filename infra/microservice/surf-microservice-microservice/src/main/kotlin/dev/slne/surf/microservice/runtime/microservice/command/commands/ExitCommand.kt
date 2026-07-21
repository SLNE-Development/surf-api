package dev.slne.surf.microservice.runtime.microservice.command.commands

import dev.slne.surf.microservice.api.microservice.command.microserviceCommand


fun exitCommand() = microserviceCommand("exit", "quit") {
    sendLine("Shutting down the microservice...")
    requestShutdown()
}