package dev.slne.surf.nats.connection

import dev.slne.surf.nats.InternalNatsApi

/**
 * Describes the state of the shared NATS connection at the time [dev.slne.surf.nats.NatsInstance.connectionHealth]
 * was called.
 *
 * The values are read from the client and do not require a round trip to the server.
 *
 * @property connectionName The connection name sent to the server: the `SURF_NATS_CONNECTION_NAME` environment
 * variable, else the configured `connection-name`, else a generated name that starts with `surf-nats-`.
 * @property connected True if [status] is `CONNECTED`, false otherwise.
 * @property status The name of the jnats `Connection.Status`, for example `CONNECTED` or `RECONNECTING`.
 */
@InternalNatsApi
data class NatsConnectionHealth(
    val connectionName: String,
    val connected: Boolean,
    val status: String,
)
