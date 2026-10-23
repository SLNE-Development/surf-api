package dev.slne.surf.nats.test.paper.event

import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import dev.slne.surf.nats.test.paper.PaperMain
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

class PaperChatListener(private val plugin: PaperMain) {
    @NatsListener
    fun onChat(event: TestChatEvent) {
        plugin.logger.info("event from ${event.origin}: ${event.message}")
        Bukkit.broadcast(Component.text("[nats event] ${event.origin}: ${event.message}"))
    }
}
