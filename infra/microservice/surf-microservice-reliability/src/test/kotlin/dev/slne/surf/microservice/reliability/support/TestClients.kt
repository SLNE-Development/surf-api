package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.nats.NatsClient
import java.util.UUID

/**
 * Creates NATS clients with unique names, so tests never collide with clients a previous test left open.
 */
object TestClients {

    /**
     * A unique client name starting with [prefix].
     */
    fun uniqueName(prefix: String): String = "$prefix-${UUID.randomUUID().toString().take(8)}"

    fun create(prefix: String): NatsClient = NatsClient.create(uniqueName(prefix))
}
