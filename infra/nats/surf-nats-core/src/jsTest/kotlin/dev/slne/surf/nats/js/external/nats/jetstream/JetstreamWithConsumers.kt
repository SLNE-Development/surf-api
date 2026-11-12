@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import dev.slne.surf.nats.js.external.nats.core.NatsConnection

@JsName("jetstream")
internal external fun jetstreamWithConsumers(nc: NatsConnection, opts: JetStreamOptions = definedExternally): JetStreamConsumerClient
