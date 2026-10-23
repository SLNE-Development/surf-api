@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.iterable.AsyncIterable

internal external fun <T> syncIterator(src: AsyncIterable<T>): SyncIterator<T>
