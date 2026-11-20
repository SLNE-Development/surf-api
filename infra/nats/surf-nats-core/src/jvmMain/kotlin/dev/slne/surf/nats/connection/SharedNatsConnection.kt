package dev.slne.surf.nats.connection

import dev.slne.surf.nats.InternalNatsApi
import org.jetbrains.annotations.Blocking

/**
 * Reference-counts the users of the connection of [NatsConnectionManager].
 *
 * The first [acquire] connects, the [release] of the last share disconnects. Every share is released exactly once by
 * its holder. Mixing shares with `NatsInstance.load` and `NatsInstance.disable` is not supported: they connect and
 * disconnect regardless of the count.
 */
@InternalNatsApi
internal object SharedNatsConnection {

    private val lock = Any()
    private var shares = 0

    /**
     * Adds a share and connects when it is the first. A failed connection adds no share.
     */
    @Blocking
    fun acquire() {
        synchronized(lock) {
            if (shares == 0) NatsConnectionManager.connect()
            shares++
        }
    }

    /**
     * Drops a share and disconnects when it was the last.
     */
    @Blocking
    fun release() {
        synchronized(lock) {
            check(shares > 0) { "No NATS connection share to release" }
            shares--
            if (shares == 0) NatsConnectionManager.disconnect()
        }
    }
}
