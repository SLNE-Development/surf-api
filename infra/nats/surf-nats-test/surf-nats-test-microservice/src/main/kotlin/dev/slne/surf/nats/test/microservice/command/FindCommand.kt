package dev.slne.surf.nats.test.microservice.command

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.nats.rpc.getOrNull
import dev.slne.surf.nats.test.common.proto.v1.FindPlayerRequest
import dev.slne.surf.nats.test.common.proto.v1.PaperTestService
import org.springframework.stereotype.Component
import kotlin.time.measureTimedValue

/**
 * `find <player>`: asks the test Paper servers where a player is.
 */
@Component
class FindCommand(private val paper: PaperTestService) : MicroserviceCommand("find") {
    override val description = "Finds a player on the test Paper servers: find <player>"

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        val (answer, took) = measureTimedValue {
            paper.findPlayer(FindPlayerRequest(args.joinToString(" ")))
        }
        sendLine("find -> ${answer.getOrNull()?.info ?: "none"} ($took)")
    }
}
