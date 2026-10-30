package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlin.time.Duration

/**
 * Messages of the RPC failures, shared by the JVM exceptions and the JS errors.
 */
@InternalNatsApi
public object NatsRpcMessages {
    public fun remote(remoteType: String, remoteMessage: String): String = "$remoteType: $remoteMessage"

    public fun noResponders(subject: String): String = "No responders for $subject"

    public fun callTimeout(subject: String, timeout: Duration): String = "No response on $subject within $timeout"
}
