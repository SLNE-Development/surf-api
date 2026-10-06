package dev.slne.surf.nats

interface NatsRegistration: AutoCloseable {
    val isActive: Boolean
    override fun close()
}