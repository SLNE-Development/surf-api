@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import kotlin.js.Promise

internal external fun jetstreamManager(nc: NatsConnection, opts: JetStreamOptions = definedExternally): Promise<JetStreamManager>
