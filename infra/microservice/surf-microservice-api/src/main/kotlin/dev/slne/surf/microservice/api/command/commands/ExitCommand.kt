package dev.slne.surf.microservice.api.command.commands

import dev.slne.surf.microservice.api.command.microserviceCommand
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.coroutines.cancel

private val log = logger()

fun exitCommand() = microserviceCommand("exit", "quit") {
    log.atInfo().log("Exit command received, shutting down...")
    cancel("Exit command executed")
}