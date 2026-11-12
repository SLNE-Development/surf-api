@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlin.js.Promise

internal external interface Consumers {
    fun get(stream: String, name: String = definedExternally): Promise<Consumer>
}
