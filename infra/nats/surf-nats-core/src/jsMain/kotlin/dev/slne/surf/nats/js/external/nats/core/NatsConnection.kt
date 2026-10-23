@file:JsModule("@nats-io/transport-node")

package dev.slne.surf.nats.js.external.nats.core

import js.iterable.AsyncIterable
import js.typedarrays.Uint8Array
import kotlin.js.Promise

internal external interface NatsConnection {
    val info: ServerInfo?
    fun publish(
        subject: String,
        payload: Uint8Array<*> = definedExternally,
        options: NatsPublishOptions = definedExternally,
    )

    fun request(
        subject: String,
        payload: Uint8Array<*> = definedExternally,
        opts: RequestOptions = definedExternally,
    ): Promise<Msg>

    fun subscribe(subject: String, opts: SubscriptionOptions = definedExternally): Subscription
    fun flush(): Promise<Unit>
    fun drain(): Promise<Unit>
    fun close(): Promise<Unit>
    fun isClosed(): Boolean
    fun status(): AsyncIterable<Status>
}
