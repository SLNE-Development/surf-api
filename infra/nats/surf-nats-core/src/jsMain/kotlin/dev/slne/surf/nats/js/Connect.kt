@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.ConnectOptions
import dev.slne.surf.nats.js.client.NatsClientFactory
import dev.slne.surf.nats.js.client.apiScope
import dev.slne.surf.nats.js.interop.requireFromJs
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * Connects a new [SurfNatsClient] to NATS.
 *
 * Every call creates an independent client with its own connection and a random instance ID. After a connection
 * loss the client reconnects without a limit on attempts. TypeScript callers see the typed signature of
 * `index.d.mts`.
 *
 * ```ts
 * import { connect } from "@slne/surf-api-nats";
 *
 * const nats = await connect({ name: "panel", servers: "nats://localhost:4222", token: "change-me" });
 * // On shutdown:
 * await nats.close();
 * ```
 *
 * @param options the client name, servers and authentication, see [ConnectOptions]
 * @return a promise of the connected client. It rejects with [IllegalArgumentException] if [options] is missing, the
 * name is not kebab-case, `servers` holds no URL, the authentication settings conflict or `connectTimeoutMs` is not
 * positive, and with the error of the file system or of nats.js if the `credsFile` cannot be read or the connection
 * fails.
 */
@JsExport
fun connect(options: ConnectOptions): Promise<SurfNatsClient> = apiScope.promise {
    SurfNatsClient(NatsClientFactory.create(requireFromJs(options, "options")))
}
