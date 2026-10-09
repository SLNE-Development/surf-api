package dev.slne.surf.nats.test.common.rpc

import dev.slne.surf.nats.Answer
import dev.slne.surf.nats.NatsService
import dev.slne.surf.nats.ResponderMode

@NatsService("paper", responders = ResponderMode.ALL)
interface PaperTestService {

    /**
     * Returns an info string about the player if online on the server, or Answer.None if not found.
     * Sent to all paper servers.
     */
    suspend fun findPlayer(name: String): Answer<String>

    /**
     * Broadcasts the provided message to all paper servers.
     */
    suspend fun broadcast(message: String)
}