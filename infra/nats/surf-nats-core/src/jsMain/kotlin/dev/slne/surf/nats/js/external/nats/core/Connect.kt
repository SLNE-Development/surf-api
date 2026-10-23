@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlin.js.Promise

internal external fun connect(opts: ConnectionOptions = definedExternally): Promise<NatsConnection>
