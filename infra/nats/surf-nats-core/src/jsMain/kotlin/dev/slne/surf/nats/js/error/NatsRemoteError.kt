@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.error

import dev.slne.surf.nats.rpc.NatsRpcMessages

/**
 * Thrown when the service implementation threw while handling the call.
 *
 * Only the exception's type name and message cross the wire, no stack trace or cause. Return expected business
 * failures in the response message instead of throwing them.
 *
 * @property type the name of the remote exception class, such as `java.lang.IllegalStateException`
 * @property remoteMessage the message of the remote exception, empty if it had none
 */
@JsExport
class NatsRemoteError internal constructor(
    val type: String,
    val remoteMessage: String,
) : NatsRpcError(NatsRpcMessages.remote(type, remoteMessage))
