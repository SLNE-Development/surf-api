package dev.slne.surf.nats

import com.google.auto.service.AutoService
import java.nio.file.Path

/**
 * Standalone platform instance whose data directory is set by [SurfNatsStandalone.start].
 */
@AutoService(NatsInstance::class)
@InternalNatsApi
internal class InstanceImpl : NatsInstance() {
    override val dataPath: Path get() = Companion.dataPath

    companion object {
        lateinit var dataPath: Path
    }
}