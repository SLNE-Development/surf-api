@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js

import dev.slne.surf.nats.protocol.ProtocolNaming
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.promise
import kotlin.js.Promise

internal val apiScope = CoroutineScope(SupervisorJob())

@JsExport
fun connect(options: ConnectOptions): Promise<SurfNatsClient> = apiScope.promise {
    SurfNatsClient(NatsRuntime.connect(requireFromJs(options, "options")))
}

/**
 * A connected surf-nats client.
 *
 * Non-global contracts are addressed under the owning client's name: pass `owner` per call or use [target].
 * `call` serves every method except `ALL_INSTANCES` methods with a result, which use `callAnswer`.
 */
@JsExport
class SurfNatsClient internal constructor(private val runtime: NatsRuntime) {
    private val operations = SurfNatsOperations(runtime, defaultOwner = null)

    val name: String get() = runtime.name
    val origin: String get() = runtime.origin
    val isClosed: Boolean get() = runtime.isClosed

    fun target(owner: String): SurfNatsTarget {
        return SurfNatsTarget(runtime, ProtocolNaming.requireClientName(requireFromJs(owner, "owner"), "owner"))
    }

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

    fun close(): Promise<Unit> = apiScope.promise { runtime.close() }
}

/**
 * Operations of a [SurfNatsClient] on the contracts owned by [owner]; an `owner` option overrides it.
 */
@JsExport
class SurfNatsTarget internal constructor(runtime: NatsRuntime, val owner: String) {
    private val operations = SurfNatsOperations(runtime, defaultOwner = owner)

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
