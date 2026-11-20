package dev.slne.surf.nats.embedded

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.config.NatsConfigLocation
import dev.slne.surf.nats.connection.NatsConnectionHealth
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.descriptor.GeneratedDescriptors
import java.nio.file.Path

/**
 * Entry points for running NATS inside a host that owns the process, such as the surf-api plugin.
 *
 * The host does not use [dev.slne.surf.nats.NatsInstance]; it creates a client per plugin and connects through
 * [NatsClientControl.connect].
 */
@InternalNatsApi
object EmbeddedNats {

    /**
     * Whether [type] is a generated service interface: an interface that declares the static `NATS_DESCRIPTOR` field
     * of a service descriptor itself.
     *
     * Reads the field declaration only, so the interface is not initialized.
     */
    fun isServiceInterface(type: Class<*>): Boolean = GeneratedDescriptors.isService(type)

    /**
     * Returns the controls of [client].
     *
     * Throws [IllegalArgumentException] for a client that was not created by `NatsClient.create`.
     */
    fun control(client: NatsClient): NatsClientControl {
        return requireNotNull(client as? NatsClientControl) { "${client.javaClass.name} is not a surf-nats core client" }
    }

    /**
     * Sets the directory that holds `config.yml`, which the shared connection reads when it first connects.
     *
     * Must be called before the first connection. Calling it again with another directory fails with
     * [IllegalStateException], because the configuration is read once per JVM.
     */
    fun useConfigDirectory(directory: Path) {
        NatsConfigLocation.install(directory)
    }

    /**
     * Returns a snapshot of the shared connection's state, or `null` while there is no connection.
     *
     * Reads the state only and never contacts the server. Unlike [dev.slne.surf.nats.NatsInstance.connectionHealth], it
     * needs no platform instance.
     */
    fun connectionHealth(): NatsConnectionHealth? = NatsConnectionManager.health()
}
