package dev.slne.surf.microservice.test.microservice

import dev.slne.surf.microservice.api.command.microserviceCommand
import dev.slne.surf.surfapi.core.api.util.logger

private val log = logger()

fun testCommand() = microserviceCommand("test") { args ->
    log.atInfo().log("Test command executed with args: ${args.joinToString(", ")}")
}