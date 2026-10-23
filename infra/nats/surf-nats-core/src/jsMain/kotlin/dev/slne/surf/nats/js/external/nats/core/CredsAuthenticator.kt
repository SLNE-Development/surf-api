@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.typedarrays.Uint8Array

internal external fun credsAuthenticator(creds: Uint8Array<*>): Authenticator
