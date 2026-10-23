@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import js.typedarrays.Uint8Array
import kotlin.js.Promise

internal external interface JetStreamClient {
    fun publish(
        subj: String,
        payload: Uint8Array<*> = definedExternally,
        options: JetStreamPublishOptions = definedExternally,
    ): Promise<PubAck>
}
