package dev.slne.surf.nats.integration

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.integration.MicroserviceIntegration
import dev.slne.surf.microservice.api.integration.MicroserviceIntegrationContext
import dev.slne.surf.nats.SurfNatsStandalone

/**
 * Connects NATS before `Microservice.onBootstrap` and disconnects after `Microservice.onDisable`.
 *
 * The configuration lives in `<dataPath>/nats/config.yml`. On shutdown, incoming events and requests stop before
 * `onDisable`, so the microservice can still publish and call services there. Runs at
 * [MicroserviceIntegration.MESSAGING_ORDER], after the database starts and before it stops.
 */
@AutoService(MicroserviceIntegration::class)
internal class NatsMicroserviceIntegration : MicroserviceIntegration {
    override val name = "nats"
    override val order = MicroserviceIntegration.MESSAGING_ORDER

    override suspend fun start(context: MicroserviceIntegrationContext) {
        SurfNatsStandalone.start(context.dataPath.resolve(CONFIG_DIRECTORY))
    }

    override suspend fun stopAccepting(context: MicroserviceIntegrationContext) {
        SurfNatsStandalone.stopAccepting()
    }

    override suspend fun stop(context: MicroserviceIntegrationContext) {
        SurfNatsStandalone.shutdown()
    }

    private companion object {
        const val CONFIG_DIRECTORY = "nats"
    }
}
