package dev.slne.surf.nats

import io.github.amadeusitgroup.testcontainers.nats.NatsContainer
import java.net.ServerSocket

object TestNatsServer {
    private val hostPort = ServerSocket(0).use { it.localPort }

    val container: NatsContainer by lazy {
        NatsContainer("nats:2-alpine")
            .withJetStream()
            .apply {
                portBindings = listOf("$hostPort:${NatsContainer.DEFAULT_NATS_CLIENT_PORT}")
                start()
            }
    }

    val url: String get() = container.connectionUrl

    fun restart() {
        container.dockerClient.restartContainerCmd(container.containerId).exec()
    }
}
