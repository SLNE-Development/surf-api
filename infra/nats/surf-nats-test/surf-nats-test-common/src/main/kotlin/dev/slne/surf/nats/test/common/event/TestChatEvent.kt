package dev.slne.surf.nats.test.common.event

import dev.slne.surf.nats.event.NatsEvent

@NatsEvent("chat")
data class TestChatEvent(val origin: String, val message: String)