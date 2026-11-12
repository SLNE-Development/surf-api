@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlin.js.Promise

/**
 * Binds `connect` of `@nats-io/transport-node`, the Node.js entry point of nats.js.
 *
 * That package re-exports the API of `@nats-io/nats-core`, so the externals of this package bind this module.
 */
internal external fun connect(opts: ConnectionOptions = definedExternally): Promise<NatsConnection>
