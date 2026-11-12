@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

// JetStreamClient with the consumers API
internal external interface JetStreamConsumerClient : JetStreamClient {
    val consumers: Consumers
}
