package dev.slne.surf.nats.test.microservice

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.nats.test.common.NatsTestInstance
import java.nio.file.Path
import kotlin.io.path.Path


@AutoService(Microservice::class)
class NatsTestMicroservice : Microservice() {
    override val dataPath: Path = Path("data")

    override suspend fun onBootstrap(args: List<String>) {
        NatsTestInstance.get().load()
        NatsTestInstance.get().enable()
    }

    override suspend fun onDisable() {
        NatsTestInstance.get().disable()
    }
}
