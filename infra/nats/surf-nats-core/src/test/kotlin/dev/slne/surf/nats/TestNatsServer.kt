package dev.slne.surf.nats

import io.github.amadeusitgroup.testcontainers.nats.NatsContainer

object TestNatsServer {
    val container: NatsContainer by lazy {
        NatsContainer("nats:2-alpine")
            .withJetStream()
            .apply { start() }
    }

    val url: String get() = container.connectionUrl
}
