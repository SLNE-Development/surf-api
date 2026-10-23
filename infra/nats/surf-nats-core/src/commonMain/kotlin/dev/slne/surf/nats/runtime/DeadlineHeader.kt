package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolHeaders
import kotlin.time.Duration

/**
 * Returns the `Surf-Deadline` value of a request that is sent now and waits [timeout].
 */
@InternalNatsApi
internal fun deadlineHeader(timeout: Duration): String = ProtocolHeaders.deadline(epochMillis(), timeout)
