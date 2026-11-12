package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.event.natsEventContext
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Listener that records every received [TestChatEvent] with its event id, duplicates included.
 */
class ReceivedEvents {
    private val received = ConcurrentLinkedQueue<Pair<String, String>>()

    /**
     * Event ids in arrival order, one entry per delivery.
     */
    val eventIds: List<String>
        get() = received.map { it.first }

    /**
     * Event messages in arrival order, one entry per delivery.
     */
    val messages: List<String>
        get() = received.map { it.second }

    @NatsListener
    suspend fun onTestChat(event: TestChatEvent) {
        received += checkNotNull(natsEventContext().eventId) { "event without id" } to event.message
    }
}
