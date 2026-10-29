@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.ConnectOptions
import dev.slne.surf.nats.js.client.NatsClientFactory
import dev.slne.surf.nats.js.client.apiScope
import dev.slne.surf.nats.js.interop.requireFromJs
import kotlinx.coroutines.promise
import kotlin.js.Promise

@JsExport
fun connect(options: ConnectOptions): Promise<SurfNatsClient> = apiScope.promise {
    SurfNatsClient(NatsClientFactory.create(requireFromJs(options, "options")))
}
