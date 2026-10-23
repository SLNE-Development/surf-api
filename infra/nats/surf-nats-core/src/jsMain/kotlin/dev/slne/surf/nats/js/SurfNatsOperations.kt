package dev.slne.surf.nats.js

import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * The operations of [SurfNatsClient] and [SurfNatsTarget]; [defaultOwner] applies when the options name no owner.
 */
internal class SurfNatsOperations(private val runtime: NatsRuntime, private val defaultOwner: String?) {

    fun publish(schema: MessageSchema, message: ProtoMessageInit?, options: PublishOptions?): Promise<Unit> {
        return apiScope.promise {
            runtime.publish(schema.asDesc(), message, options?.owner ?: defaultOwner, options?.timeoutMs)
        }
    }

    fun listen(
        schema: MessageSchema,
        handler: (ProtoMessage, EventContext) -> Any?,
        options: ListenOptions?,
    ): Promise<NatsRegistration> = apiScope.promise {
        runtime.listen(schema.asDesc(), requireFromJs(handler, "handler"), options?.owner ?: defaultOwner, options)
    }

    fun call(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions?): Promise<ProtoMessage?> {
        return apiScope.promise {
            runtime.call(method.asDesc(), request, options?.owner ?: defaultOwner, options)
        }
    }

    fun callAnswer(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions?): Promise<Answer> {
        return apiScope.promise {
            runtime.callAnswer(method.asDesc(), request, options?.owner ?: defaultOwner, options)
        }
    }
}
