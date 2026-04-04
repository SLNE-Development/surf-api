package dev.slne.surf.microservice.runtime.microservice.command.commands

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.microservice.api.microservice.command.microserviceCommand
import kotlinx.coroutines.cancel

private val log = logger()

fun exitCommand() = microserviceCommand("exit", "quit") {
    log.atInfo().log("Exit command received, shutting down...")
    cancel("Exit command executed")
}