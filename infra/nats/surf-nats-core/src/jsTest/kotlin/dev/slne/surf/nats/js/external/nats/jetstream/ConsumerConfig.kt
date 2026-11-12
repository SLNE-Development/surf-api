@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlinx.js.JsPlainObject

// Partial<ConsumerConfig>
@Suppress("PropertyName")
@JsPlainObject
internal external interface ConsumerConfig {
    val durable_name: String?
    val ack_policy: String?
}
