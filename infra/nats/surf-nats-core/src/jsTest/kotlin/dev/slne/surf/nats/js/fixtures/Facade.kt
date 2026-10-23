@file:JsModule("../index.mjs")

package dev.slne.surf.nats.js.fixtures.facade

import dev.slne.surf.nats.js.SurfNatsClient
import dev.slne.surf.nats.js.fixtures.Uuid
import kotlin.js.Promise

// ConnectOptions of the TypeScript facade, with the string form of servers.
internal external interface FacadeConnectOptions {
    var name: String
    var servers: String?
}

internal external fun connect(options: FacadeConnectOptions?): Promise<SurfNatsClient>
internal external fun uuidToString(uuid: Any?): String
internal external fun uuidFromString(value: String?): Uuid
internal external val NatsRpcError: Any
internal external val NatsRemoteError: Any
internal external val NatsNoRespondersError: Any
internal external val NatsCallTimeoutError: Any
