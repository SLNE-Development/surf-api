@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

/**
 * Binds `Authenticator`, which nats.js calls with the server nonce during the connection handshake.
 *
 * Obtain one from [credsAuthenticator].
 */
internal external interface Authenticator
