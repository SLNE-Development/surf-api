package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.event.natsEventContext
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Listener that records the origin of every received [TestChatEvent], once through a listener method that sees all
 * events and once through one that skips the events of its own client.
 */
class OriginListener {

    /**
     * One entry per delivery to the listener that sees all events.
     */
    val all = ConcurrentLinkedQueue<ReceivedOrigin>()

    /**
     * One entry per delivery to the listener with `ignoreFromThisClient`.
     */
    val ignoringOwn = ConcurrentLinkedQueue<ReceivedOrigin>()

    @NatsListener
    suspend fun onAny(event: TestChatEvent) {
        all += received(event)
    }

    @NatsListener(ignoreFromThisClient = true)
    suspend fun onForeign(event: TestChatEvent) {
        ignoringOwn += received(event)
    }

    private suspend fun received(event: TestChatEvent): ReceivedOrigin {
        val context = natsEventContext()
        return ReceivedOrigin(event.message, context.origin, context.isFromThisClient)
    }
}
