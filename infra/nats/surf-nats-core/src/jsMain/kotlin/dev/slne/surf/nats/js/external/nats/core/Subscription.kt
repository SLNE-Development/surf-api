@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import kotlin.js.Promise

internal external interface Subscription {
    fun unsubscribe(max: Int = definedExternally)
    fun drain(): Promise<Unit>
    fun isClosed(): Boolean
    fun getSubject(): String
}
