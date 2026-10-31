@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.api.CallOptions
import dev.slne.surf.nats.js.api.EventContext
import dev.slne.surf.nats.js.api.ListenOptions
import dev.slne.surf.nats.js.api.MessageSchema
import dev.slne.surf.nats.js.api.MethodSchema
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.api.PublishOptions
import dev.slne.surf.nats.js.client.NatsClientImpl
import dev.slne.surf.nats.js.client.apiScope
import dev.slne.surf.nats.js.event.NatsRegistration
import dev.slne.surf.nats.js.interop.asDesc
import dev.slne.surf.nats.js.interop.requireFromJs
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * A connected surf-nats client.
 *
 * Every operation addresses its contract through the proto full name of the schema.
 * `call` serves every method except `ALL_INSTANCES` methods with a result, which use `callAnswer`.
 */
@JsExport
class SurfNatsClient internal constructor(private val client: NatsClientImpl) {
    val name: String get() = client.name
    val origin: String get() = client.origin
    val isClosed: Boolean get() = client.isClosed

    fun publish(schema: MessageSchema, message: ProtoMessageInit?, options: PublishOptions? = null): Promise<Unit> {
        return apiScope.promise { client.publish(schema.asDesc(), message, options?.timeoutMs) }
    }

    fun listen(
        schema: MessageSchema,
        handler: (ProtoMessage, EventContext) -> Any?,
        options: ListenOptions? = null,
    ): Promise<NatsRegistration> = apiScope.promise {
        client.registerListener(schema.asDesc(), requireFromJs(handler, "handler"), options)
    }

    fun call(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions? = null): Promise<ProtoMessage?> {
        return apiScope.promise { client.call(method.asDesc(), request, options?.timeoutMs) }
    }

    fun callAnswer(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions? = null): Promise<Answer> {
        return apiScope.promise { client.callAnswer(method.asDesc(), request, options?.timeoutMs) }
    }

    fun close(): Promise<Unit> = apiScope.promise { client.close() }
}
