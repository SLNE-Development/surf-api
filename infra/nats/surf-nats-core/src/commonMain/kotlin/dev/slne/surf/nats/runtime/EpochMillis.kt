package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

/**
 * Returns the current wall-clock time in epoch milliseconds.
 */
@InternalNatsApi
internal expect fun epochMillis(): Long
