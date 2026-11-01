package dev.slne.surf.nats.test.paper.rpc

import dev.slne.surf.nats.rpc.Answer
import dev.slne.surf.nats.test.common.proto.v1.BroadcastRequest
import dev.slne.surf.nats.test.common.proto.v1.FindPlayerRequest
import dev.slne.surf.nats.test.common.proto.v1.FindPlayerResponse
import dev.slne.surf.nats.test.common.proto.v1.PaperTestService
import dev.slne.surf.nats.test.paper.PaperMain
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

class PaperTestServiceImpl(private val plugin: PaperMain) : PaperTestService {
    override suspend fun findPlayer(request: FindPlayerRequest): Answer<FindPlayerResponse> {
        val player = Bukkit.getPlayerExact(request.name) ?: return Answer.Unknown
        return Answer.Known(FindPlayerResponse(info = "${player.name} on ${plugin.origin}"))
    }

    override suspend fun broadcast(request: BroadcastRequest) {
        val message = request.message
        plugin.logger.info("broadcast: $message")
        Bukkit.broadcast(Component.text("[nats broadcast] $message"))
    }
}
