@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlin.js.Promise

// ConsumerAPI
internal external interface ConsumerApi {
    fun add(stream: String, cfg: ConsumerConfig): Promise<ConsumerInfo>
}
