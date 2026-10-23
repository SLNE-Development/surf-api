package dev.slne.surf.nats

import java.nio.file.Path

object SurfNatsStandalone {

    suspend fun start(dataPath: Path) {
        InstanceImpl.dataPath = dataPath
        NatsInstance.instance.load()
    }

    suspend fun shutdown() {
        NatsInstance.instance.disable()
    }
}