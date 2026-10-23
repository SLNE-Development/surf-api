package dev.slne.surf.nats.connection

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
data class NatsConnectionHealth(
    val connectionName: String,
    val connected: Boolean,
    val status: String,
)
