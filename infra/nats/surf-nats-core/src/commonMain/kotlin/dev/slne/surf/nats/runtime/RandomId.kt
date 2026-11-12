package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import kotlin.uuid.Uuid

/**
 * Returns a random UUID in `8-4-4-4-12` form, used for instance ids, `Nats-Msg-Id` and payload object names.
 */
@InternalNatsApi
internal fun randomId(): String = Uuid.random().toHexDashString()