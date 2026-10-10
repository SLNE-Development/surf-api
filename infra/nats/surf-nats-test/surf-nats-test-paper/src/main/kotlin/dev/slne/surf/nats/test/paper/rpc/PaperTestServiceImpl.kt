package dev.slne.surf.nats.test.paper.rpc

import dev.slne.surf.nats.rpc.Answer
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.test.common.event.TestChatEvent
import dev.slne.surf.nats.test.common.rpc.PaperTestService
import dev.slne.surf.nats.test.paper.PaperMain
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

class PaperTestServiceImpl(private val plugin: PaperMain) : PaperTestService {
    override suspend fun findPlayer(name: String): Answer<String> {
        val player = Bukkit.getPlayerExact(name) ?: return Answer.None
        return Answer.Of("${player.name} on ${plugin.origin}")
    }

    override suspend fun broadcast(message: String) {
        plugin.logger.info("broadcast: $message")
        Bukkit.broadcast(Component.text("[nats broadcast] $message"))
    }
}

class PaperChatListener(private val plugin: PaperMain) {
    @NatsListener
    fun onChat(event: TestChatEvent) {
        plugin.logger.info("event from ${event.origin}: ${event.message}")
        Bukkit.broadcast(Component.text("[nats event] ${event.origin}: ${event.message}"))
    }
}