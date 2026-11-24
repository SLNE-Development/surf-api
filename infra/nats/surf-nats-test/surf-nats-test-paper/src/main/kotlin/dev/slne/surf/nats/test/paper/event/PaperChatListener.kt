package dev.slne.surf.nats.test.paper.event

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.springframework.stereotype.Component as SpringComponent

/**
 * Broadcasts every [TestChatEvent] published by a test command or by the test microservice.
 */
@SpringComponent
class PaperChatListener {
    private val log = logger()

    @NatsListener
    fun onChat(event: TestChatEvent) {
        log.atInfo()
            .log("event from %s: %s", event.origin, event.message)
        Bukkit.broadcast(Component.text("[nats event] ${event.origin}: ${event.message}"))
    }
}
