@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.typedarrays.Uint8Array

/**
 * Binds `credsAuthenticator`, which creates an [Authenticator] from the content of a `.creds` file.
 */
internal external fun credsAuthenticator(creds: Uint8Array<*>): Authenticator
