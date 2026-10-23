package dev.slne.surf.nats

import com.google.auto.service.AutoService
import java.nio.file.Path

@AutoService(NatsInstance::class)
@InternalNatsApi
internal class InstanceImpl : NatsInstance() {
    override val dataPath: Path get() = Companion.dataPath

    companion object {
        lateinit var dataPath: Path
    }
}