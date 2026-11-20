package dev.slne.surf.nats.test.microservice.command

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.nats.test.common.proto.v1.BroadcastRequest
import dev.slne.surf.nats.test.common.proto.v1.PaperTestService
import org.springframework.stereotype.Component

/**
 * `broadcast <message>`: has the test Paper servers broadcast a message.
 */
@Component
class BroadcastCommand(private val paper: PaperTestService) : MicroserviceCommand("broadcast") {
    override val description = "Broadcasts a message on the test Paper servers: broadcast <message>"

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        paper.broadcast(BroadcastRequest(args.joinToString(" ")))
        sendLine("broadcast sent")
    }
}
