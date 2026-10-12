package dev.slne.surf.nats.test.common.rpc

import dev.slne.surf.nats.rpc.Answer
import dev.slne.surf.nats.rpc.NatsRecipients
import dev.slne.surf.nats.rpc.NatsService
import dev.slne.surf.nats.rpc.Recipients

@NatsService("paper")
@NatsRecipients(Recipients.ALL_INSTANCES)
interface PaperTestService {

    /**
     * Returns an info string about the player if online on the server, or Answer.Unknown if not found.
     * Sent to all paper servers.
     */
    suspend fun findPlayer(name: String): Answer<String>

    /**
     * Broadcasts the provided message to all paper servers.
     */
    suspend fun broadcast(message: String)
}