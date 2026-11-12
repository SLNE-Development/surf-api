@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

// JetStream API used by the e2e tests to play the JVM responder side.

internal external interface JetStreamManager {
    val streams: StreamApi
    val consumers: ConsumerApi
}
