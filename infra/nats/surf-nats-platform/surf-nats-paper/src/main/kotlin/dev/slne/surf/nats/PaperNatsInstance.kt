package dev.slne.surf.nats

import com.google.auto.service.AutoService
import java.nio.file.Path

@InternalNatsApi
@AutoService(NatsInstance::class)
class PaperNatsInstance : NatsInstance() {
    override lateinit var dataPath: Path

    companion object {
        fun get() = instance as PaperNatsInstance
    }
}