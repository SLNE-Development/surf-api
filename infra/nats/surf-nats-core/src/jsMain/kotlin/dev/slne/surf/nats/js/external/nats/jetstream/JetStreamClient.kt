@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import js.typedarrays.Uint8Array
import kotlin.js.Promise

/**
 * Binds the members of `JetStreamClient` that surf-nats uses.
 */
internal external interface JetStreamClient {
    fun publish(
        subj: String,
        payload: Uint8Array<*> = definedExternally,
        options: JetStreamPublishOptions = definedExternally,
    ): Promise<PubAck>
}
