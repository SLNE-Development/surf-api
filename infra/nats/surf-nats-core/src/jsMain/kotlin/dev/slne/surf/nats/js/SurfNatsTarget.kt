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
import dev.slne.surf.nats.js.event.NatsRegistration
import kotlin.js.Promise

/**
 * Operations of a [SurfNatsClient] on the contracts owned by [owner]; an `owner` option overrides it.
 */
@JsExport
class SurfNatsTarget internal constructor(client: NatsClientImpl, val owner: String) {
    private val operations = SurfNatsOperations(client, defaultOwner = owner)

    fun publish(schema: MessageSchema, message: ProtoMessageInit?, options: PublishOptions? = null): Promise<Unit> {
        return operations.publish(schema, message, options)
    }

    fun listen(
        schema: MessageSchema,
        handler: (ProtoMessage, EventContext) -> Any?,
        options: ListenOptions? = null,
    ): Promise<NatsRegistration> = operations.listen(schema, handler, options)

    fun call(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions? = null): Promise<ProtoMessage?> {
        return operations.call(method, request, options)
    }

    fun callAnswer(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions? = null): Promise<Answer> {
        return operations.callAnswer(method, request, options)
    }
}
