package dev.slne.surf.nats

import java.nio.file.Path

/**
 * Lifecycle of the NATS connection in a standalone application.
 *
 * Microservices do not call this directly: [dev.slne.surf.nats.integration.NatsMicroserviceIntegration] drives it.
 */
object SurfNatsStandalone {

    /**
     * Connects to NATS with the configuration in `<dataPath>/config.yml`.
     */
    suspend fun start(dataPath: Path) {
        InstanceImpl.dataPath = dataPath
        NatsInstance.instance.load()
    }

    /**
     * Stops all clients from receiving events and requests; publishing and service calls keep working until
     * [shutdown].
     */
    suspend fun stopAccepting() {
        NatsInstance.instance.stopAccepting()
    }

    /**
     * Closes all clients and disconnects.
     */
    suspend fun shutdown() {
        NatsInstance.instance.disable()
    }
}
