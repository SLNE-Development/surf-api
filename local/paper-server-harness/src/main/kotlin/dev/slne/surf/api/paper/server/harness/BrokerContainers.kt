package dev.slne.surf.api.paper.server.harness

import org.junit.jupiter.api.Assumptions.abort
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * The Redis and NATS a server with surf-api connects to, each in a Docker container of its own.
 *
 * [environment] carries their addresses under the variable names surf-api's configuration reads,
 * so the server needs no configuration file. The containers outlive server runs: stopping, killing
 * and restarting a [PaperServer] keeps them.
 */
class BrokerContainers private constructor(
    private val redis: GenericContainer<*>,
    private val nats: GenericContainer<*>,
) : AutoCloseable {

    val environment: Map<String, String> = mapOf(
        "SURF_REDIS_HOST" to redis.host,
        "SURF_REDIS_PORT" to redis.getMappedPort(REDIS_PORT).toString(),
        "SURF_NATS_SERVERS" to "nats://${nats.host}:${nats.getMappedPort(NATS_PORT)}",
    )

    override fun close() {
        nats.stop()
        redis.stop()
    }

    companion object {

        private const val REDIS_PORT = 6379
        private const val NATS_PORT = 4222

        /**
         * Starts both containers, or aborts the test when Docker is not usable. A machine without
         * Docker is not a defect in this repository.
         */
        fun start(): BrokerContainers {
            if (!DockerClientFactory.instance().isDockerAvailable) {
                abort<Nothing>("Docker is not available, so Redis and NATS cannot be started.")
            }

            val redis = GenericContainer("redis:7")
                .withExposedPorts(REDIS_PORT)
                .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))
            val nats = GenericContainer("nats:2")
                .withCommand("-js")
                .withExposedPorts(NATS_PORT)
                .waitingFor(Wait.forLogMessage(".*Server is ready.*", 1))

            try {
                redis.start()
                nats.start()
            } catch (failure: RuntimeException) {
                nats.stop()
                redis.stop()
                abort<Nothing>("Redis and NATS could not be started in Docker: $failure")
            }

            return BrokerContainers(redis, nats)
        }
    }
}
