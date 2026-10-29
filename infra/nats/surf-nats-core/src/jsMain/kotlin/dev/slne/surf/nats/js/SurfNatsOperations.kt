package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.api.CallOptions
import dev.slne.surf.nats.js.api.ListenOptions
import dev.slne.surf.nats.js.api.MessageSchema
import dev.slne.surf.nats.js.api.MethodSchema
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.api.PublishOptions
import dev.slne.surf.nats.js.client.NatsClientImpl
import dev.slne.surf.nats.js.client.apiScope
import dev.slne.surf.nats.js.event.EventHandler
import dev.slne.surf.nats.js.event.NatsRegistration
import dev.slne.surf.nats.js.interop.asDesc
import dev.slne.surf.nats.js.interop.requireFromJs
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * The operations of [SurfNatsClient] and [SurfNatsTarget]; [defaultOwner] applies when the options name no owner.
 */
internal class SurfNatsOperations(private val client: NatsClientImpl, private val defaultOwner: String?) {
    fun publish(schema: MessageSchema, event: ProtoMessageInit?, options: PublishOptions?): Promise<Unit> {
        return apiScope.promise {
            client.publish(schema.asDesc(), event, ownerOf(options?.owner), options?.timeoutMs)
        }
    }

    fun listen(schema: MessageSchema, handler: EventHandler, options: ListenOptions?): Promise<NatsRegistration> {
        return apiScope.promise {
            client.registerListener(schema.asDesc(), requireFromJs(handler, "handler"), ownerOf(options?.owner), options)
        }
    }

    fun call(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions?): Promise<ProtoMessage?> {
        return apiScope.promise {
            client.call(method.asDesc(), request, ownerOf(options?.owner), options?.timeoutMs)
        }
    }

    fun callAnswer(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions?): Promise<Answer> {
        return apiScope.promise {
            client.callAnswer(method.asDesc(), request, ownerOf(options?.owner), options?.timeoutMs)
        }
    }

    private fun ownerOf(owner: String?): String? = owner ?: defaultOwner
}
