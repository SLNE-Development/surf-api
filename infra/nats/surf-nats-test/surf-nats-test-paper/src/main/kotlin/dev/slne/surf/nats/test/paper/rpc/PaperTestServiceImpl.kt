package dev.slne.surf.nats.test.paper.rpc

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.nats.NatsService
import dev.slne.surf.nats.rpc.Answer
import dev.slne.surf.nats.test.common.proto.v1.BroadcastRequest
import dev.slne.surf.nats.test.common.proto.v1.FindPlayerRequest
import dev.slne.surf.nats.test.common.proto.v1.FindPlayerResponse
import dev.slne.surf.nats.test.common.proto.v1.PaperTestService
import dev.slne.surf.nats.test.paper.serverOrigin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

/**
 * Serves [PaperTestService] for this server: answers player lookups and broadcasts messages.
 */
@NatsService
class PaperTestServiceImpl : PaperTestService {
    private val log = logger()

    override suspend fun findPlayer(request: FindPlayerRequest): Answer<FindPlayerResponse> {
        val player = Bukkit.getPlayerExact(request.name) ?: return Answer.Unknown
        return Answer.Known(FindPlayerResponse(info = "${player.name} on $serverOrigin"))
    }

    override suspend fun broadcast(request: BroadcastRequest) {
        val message = request.message
        log.atInfo().log("broadcast: %s", message)
        Bukkit.broadcast(Component.text("[nats broadcast] $message"))
    }
}
