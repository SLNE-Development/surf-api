package dev.slne.surf.nats

import org.jetbrains.annotations.ApiStatus

@ApiStatus.NonExtendable
interface NatsRegistration: AutoCloseable {
    val isActive: Boolean
    override fun close()
}
