package dev.slne.surf.nats

import com.google.auto.service.AutoService
import java.nio.file.Path

@AutoService(NatsInstance::class)
@InternalNatsApi
class VelocityNatsInstance : NatsInstance() {
    override val dataPath: Path
        get() = plugin.dataPath
}