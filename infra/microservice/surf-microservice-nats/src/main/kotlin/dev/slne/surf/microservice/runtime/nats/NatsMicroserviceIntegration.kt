package dev.slne.surf.microservice.runtime.nats

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.microservice.api.microservice.integration.MicroserviceIntegration
import dev.slne.surf.nats.SurfNatsStandalone

@AutoService(MicroserviceIntegration::class)
class NatsMicroserviceIntegration : MicroserviceIntegration {
    override val name = "surf-nats"

    override suspend fun start(microservice: Microservice) {
        SurfNatsStandalone.start(microservice.dataPath.resolve("nats"))
    }

    override suspend fun stop(microservice: Microservice) {
        SurfNatsStandalone.shutdown()
    }
}
