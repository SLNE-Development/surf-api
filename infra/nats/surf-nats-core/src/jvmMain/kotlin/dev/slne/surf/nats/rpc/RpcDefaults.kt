package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolDefaults
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Timing and delivery limits of service calls on the JVM runtime.
 *
 * The `var` properties are mutable only so tests can shorten them.
 */
@InternalNatsApi
internal object RpcDefaults {
    val HEARTBEAT_INTERVAL = ProtocolDefaults.PRESENCE_HEARTBEAT_SECONDS.seconds
    val PRESENCE_TTL = ProtocolDefaults.PRESENCE_TTL_SECONDS.seconds

    /**
     * Time after a presence query during which an `ALL_INSTANCES` call with an answer does not end early, so the
     * instances can announce themselves first.
     */
    val PRESENCE_WARMUP = 1.seconds

    /**
     * Maximum wait for the server to confirm the acknowledgement of a delivery whose payload is deleted afterwards.
     */
    val ACK_SYNC_TIMEOUT = 5.seconds
    /**
     * Ack wait of the durable consumer. A running or waiting persistent delivery extends it every half interval.
     */
    @Volatile
    internal var ACK_WAIT = 10.seconds // var for tests
    /**
     * Time limit of a persistent fire handler. A handler that exceeds it is cancelled cooperatively and counts as
     * failed with a [java.util.concurrent.TimeoutException]. A fire call without `Surf-Deadline` also waits at most
     * this long for a handler slot.
     */
    @Volatile
    internal var FIRE_MAX_DURATION = 10.minutes // var for tests

    /**
     * Maximum number of deliveries of a persistent message, set on the durable consumer. A fire that still fails on
     * the last delivery is dead-lettered.
     */
    const val MAX_DELIVER = 5

    /**
     * Default limit of concurrently running handlers per service registration: twice the default database pool size
     * of surf-database-r2dbc, so connection waits stay short and admission order is decided by [DeadlineLimiter].
     */
    const val MAX_CONCURRENT_HANDLERS = 20

    /**
     * Redelivery delay of a persistent delivery that waited for a handler slot when its registration started
     * draining. Short, so another instance takes it over soon.
     */
    val DRAIN_REDELIVERY_DELAY = 1.seconds

    /**
     * Interval at which a service registration summarizes the requests that got no handler slot.
     */
    val NOT_ADMITTED_LOG_INTERVAL = 10.seconds

    /**
     * Redelivery delay after each failed delivery, indexed by delivery number. The last entry applies to all later
     * deliveries.
     */
    @Volatile
    internal var NAK_BACKOFF = listOf(1.seconds, 5.seconds, 30.seconds, 2.minutes) // var for tests
}
