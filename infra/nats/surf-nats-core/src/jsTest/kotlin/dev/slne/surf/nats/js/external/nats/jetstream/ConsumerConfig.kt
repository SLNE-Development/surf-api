@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

// Partial<ConsumerConfig>
@Suppress("PropertyName")
internal external interface ConsumerConfig {
    var durable_name: String?
    var ack_policy: String?
}
