@file:JsModule("@nats-io/jetstream")

package dev.slne.surf.nats.js.external.nats.jetstream

import kotlin.js.Promise

internal external interface Consumer {
    fun next(opts: NextOptions = definedExternally): Promise<JsMsg?>
}
