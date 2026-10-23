package dev.slne.surf.nats.test.microservice.event

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent

object ChatListener {
    private val log = logger()

    @NatsListener
    fun onTestChat(event: TestChatEvent) {
        log.atInfo()
            .log("event from %s: %s", event.origin, event.message)
    }
}