@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlin.js.Promise

// StreamAPI
internal external interface StreamApi {
    fun add(cfg: StreamConfig): Promise<StreamInfo>
    fun info(stream: String): Promise<StreamInfo>
    fun delete(stream: String): Promise<Boolean>
}
