import dev.slne.surf.microservice.api.command.microserviceCommand
import dev.slne.surf.microservice.api.log

fun testCommand() = microserviceCommand("test") { args ->
    log.atInfo().log("Test command executed with args: ${args.joinToString(", ")}")
}