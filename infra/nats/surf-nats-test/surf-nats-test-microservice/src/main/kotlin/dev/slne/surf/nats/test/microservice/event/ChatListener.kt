package dev.slne.surf.nats.test.microservice.event

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import org.springframework.stereotype.Component

/**
 * Logs every [TestChatEvent] published by a test command or by a test Paper server.
 */
@Component
class ChatListener {
    private val log = logger()

    @NatsListener
    fun onTestChat(event: TestChatEvent) {
        log.atInfo()
            .log("event from %s: %s", event.origin, event.message)
    }
}
