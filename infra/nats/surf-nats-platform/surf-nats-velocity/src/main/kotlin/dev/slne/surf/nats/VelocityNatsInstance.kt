package dev.slne.surf.nats

import com.google.auto.service.AutoService
import java.nio.file.Path

/**
 * Velocity platform instance that uses the plugin data directory.
 */
@AutoService(NatsInstance::class)
@InternalNatsApi
class VelocityNatsInstance : NatsInstance() {
    override val dataPath: Path
        get() = plugin.dataPath
}