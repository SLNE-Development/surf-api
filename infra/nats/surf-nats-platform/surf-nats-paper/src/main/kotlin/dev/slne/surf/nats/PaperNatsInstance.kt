package dev.slne.surf.nats

import com.google.auto.service.AutoService
import java.nio.file.Path

/**
 * Paper platform instance whose data directory is set by [PaperBootstrap] before [load].
 */
@InternalNatsApi
@AutoService(NatsInstance::class)
class PaperNatsInstance : NatsInstance() {
    override lateinit var dataPath: Path

    companion object {
        /**
         * Returns the registered instance cast to [PaperNatsInstance].
         */
        fun get() = instance as PaperNatsInstance
    }
}