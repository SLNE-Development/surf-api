package dev.slne.surf.nats.test.microservice

import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.nats.test.common.NatsTestInstance
import java.nio.file.Path
import kotlin.io.path.Path

@Suppress("unused")
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
