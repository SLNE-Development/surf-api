package dev.slne.surf.nats

import org.jetbrains.annotations.ApiStatus

@ApiStatus.NonExtendable
public interface NatsRegistration: AutoCloseable {
    public val isActive: Boolean
    override fun close()
}
