@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import dev.slne.surf.nats.js.external.nats.core.NatsConnection

internal external fun jetstream(nc: NatsConnection, opts: JetStreamOptions = definedExternally): JetStreamClient
