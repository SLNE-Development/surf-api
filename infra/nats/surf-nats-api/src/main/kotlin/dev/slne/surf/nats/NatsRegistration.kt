package dev.slne.surf.nats

import org.jetbrains.annotations.ApiStatus

@ApiStatus.NonExtendable
public interface NatsRegistration: AutoCloseable {
    public val isClosed: Boolean
    override fun close()
}
