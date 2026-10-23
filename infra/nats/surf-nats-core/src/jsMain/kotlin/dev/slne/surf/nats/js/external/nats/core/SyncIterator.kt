@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlin.js.Promise

internal external interface SyncIterator<T> {
    // Resolves to null once the source is closed.
    fun next(): Promise<T?>
}
