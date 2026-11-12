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
 * A connected surf-nats client for Node.js, created by [connect].
 *
 * The client publishes and listens to events and calls service methods, addressed by their protobuf-es schemas.
 * Each client owns one NATS connection. Events and services must be declared in a proto package that ends with a
 * major version such as `.v1`. Their surf options are read only when the declaring file imports
 * `surf/nats/v1/options.proto` directly: without it a message is no event and service options count as unset.
 * Outgoing messages are checked before anything is sent: every proto2 `required` field must be set, also in nested
 * messages, and a `surf.types.v1.Uuid` must hold 16 bytes. Incoming messages pass the same checks, and enum numbers
 * the schema does not know become the enum's zero value, or its first value if it declares no zero. TypeScript
 * callers see the typed signatures of `index.d.mts`.
 *
 * ```ts
 * const registration = await nats.listen(PlayerJoinedSchema, (event, context) => {
 *     console.log(`${event.name} joined, published by ${context.origin}`);
 * });
 * nats.publish(PlayerJoinedSchema, { name: "Steve" });
 *
 * const player = await nats.call(PlayerService.method.findPlayer, { name: "Steve" });
 * const located = await nats.callAnswer(PlayerService.method.locate, { name: "Steve" });
 *
 * registration.close();
 * await nats.close();
 * ```
 */
@JsExport
class SurfNatsClient internal constructor(private val client: NatsClientImpl) {
    /**
     * The client name from [ConnectOptions.name][dev.slne.surf.nats.js.api.ConnectOptions.name].
     */
    val name: String get() = client.name

    /**
     * The origin this client sends with its messages: `<name>@<instanceId>`, with a random instance ID per [connect].
     */
    val origin: String get() = client.origin

    /**
     * `true` once [close] has completed, `false` while the client is open or closing.
     */
    val isClosed: Boolean get() = client.isClosed

    /**
     * Publishes an event without waiting for the server, like `NatsClient.publish` on the JVM.
     *
     * The connection buffers the event, and [close] sends the buffered events before it ends. Delivery is not
     * guaranteed: a listener that is offline when the event arrives misses it. Events are never moved to the object
     * store, so an event above the server's max payload is rejected. Handlers that run while the client closes can
     * still publish.
     *
     * ```ts
     * nats.publish(ServerStartedSchema, { server: "lobby-1" }, { eventId: "lobby-1-started" });
     * ```
     *
     * @param schema the schema of the event, a message with `option (surf.nats.v1.event) = true`
     * @param message the event, as a message of [schema] or as an init object that is converted with `create`.
     * `null` or `undefined` publishes the default message.
     * @param options the event ID, see [PublishOptions]
     * @throws IllegalArgumentException if [schema] is no event, [message] is invalid or of another type, the event ID
     * is invalid or the event exceeds the server's max payload. Nothing is sent then.
     * @throws IllegalStateException if the client is closed
     */
    fun publish(schema: MessageSchema, message: ProtoMessageInit?, options: PublishOptions? = null) {
        client.publish(schema.asDesc(), message, options?.eventId)
    }

    /**
     * Registers [handler] as a listener for the event of [schema].
     *
     * The promise resolves once the server knows the subscription, so the listener receives every event published
     * after that. The handler is called once per event with the decoded message and its [EventContext]. A returned
     * promise or thenable is not awaited before the next event is delivered, and [close] waits for it for a bounded
     * time. A thrown error or a rejection is logged to the console and does not affect other listeners. Events that
     * cannot be decoded, that have no protobuf content type or that lack a `required` field are dropped with a
     * warning.
     *
     * ```ts
     * const registration = await nats.listen(PlayerJoinedSchema, (event) => {
     *     console.log(`${event.name} joined`);
     * }, { recipients: "ANY_INSTANCE" });
     * ```
     *
     * @param schema the schema of the event, a message with `option (surf.nats.v1.event) = true`
     * @param handler the function that receives each event
     * @param options which listeners receive an event and whether to skip own events, see [ListenOptions]
     * @return a promise of the registration that stops the listener. It rejects with [IllegalArgumentException] if
     * [schema] is no event or the options are invalid, and with [IllegalStateException] if another schema object for
     * an event with the same full name already has a listener on this client or the client is closing or closed.
     */
    fun listen(
        schema: MessageSchema,
        handler: (ProtoMessage, EventContext) -> Any?,
        options: ListenOptions? = null,
    ): Promise<NatsRegistration> = apiScope.promise {
        client.registerListener(schema.asDesc(), requireFromJs(handler, "handler"), options)
    }

    /**
     * Calls the unary service [method] and resolves with its response.
     *
     * Handles every method except `ALL_INSTANCES` methods with a response message, which need [callAnswer]. What the
     * promise waits for depends on the method:
     *
     * - `ANY_INSTANCE`: one instance handles the request. The promise resolves with its response, or after the
     *   handler completed for a method that returns `google.protobuf.Empty`.
     * - `ALL_INSTANCES` returning `google.protobuf.Empty`: every instance receives the request. The promise resolves
     *   once the connection has buffered the request, without waiting for the server, and reports no failure of an
     *   instance.
     * - Persistent, returning `google.protobuf.Empty`: the promise resolves once JetStream stored the request. The
     *   request is sent again with the same `Nats-Msg-Id` until JetStream confirms it.
     * - Persistent, with a response message: JetStream stores the request and the promise resolves with the response
     *   of the instance that handled it.
     *
     * Requests and responses above the server's max payload go through the object store, which requires JetStream.
     * Missing object store buckets are created, streams never. Except for persistent methods without a result, a
     * request carries the absolute time at which the call expires, so the hosts need synchronized clocks.
     *
     * ```ts
     * const player = await nats.call(PlayerService.method.findPlayer, { name: "Steve" });
     * await nats.call(PlayerService.method.kick, { name: "Steve" }, { timeoutMs: 2000 });
     * ```
     *
     * @param method the method descriptor that protoc-gen-es generates, such as `PlayerService.method.findPlayer`
     * @param request the request, as a message or as an init object that is converted with `create`
     * @param options the timeout and the idempotency key, see [CallOptions]
     * @return a promise of the response message, or of `undefined` for a method that returns
     * `google.protobuf.Empty`. It rejects with a [NatsRpcError][dev.slne.surf.nats.js.error.NatsRpcError] or one of
     * its subtypes if the call failed, with [IllegalArgumentException] if the method is invalid or needs
     * [callAnswer] or the request or options are invalid, and with [IllegalStateException] if the client is closed or
     * a payload needs the object store on a server without JetStream. A response that cannot be decoded rejects with
     * an `Error`.
     */
    fun call(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions? = null): Promise<ProtoMessage?> {
        return apiScope.promise { client.call(method.asDesc(), request, options) }
    }

    /**
     * Calls the `ALL_INSTANCES` [method] with a response message on every instance and resolves with the first known
     * answer.
     *
     * An instance answers with a value or marks its answer as unknown. The client does not track which instances
     * are running, so without a known answer the promise resolves only after the timeout, unless no instance serves
     * the method. A timeout never rejects with `NatsCallTimeoutError`.
     *
     * ```ts
     * const located = await nats.callAnswer(PlayerService.method.locate, { name: "Steve" }, { timeoutMs: 1000 });
     * console.log(located.known ? `on ${located.value.server}` : "offline");
     * ```
     *
     * @param method the method descriptor that protoc-gen-es generates, such as `PlayerService.method.locate`
     * @param request the request, as a message or as an init object that is converted with `create`
     * @param options the timeout, see [CallOptions]
     * @return a promise of `{ known: true, value }` with the first known answer, or of `{ known: false }`. Without a
     * known answer it rejects with the first failure of an instance, a
     * [NatsRemoteError][dev.slne.surf.nats.js.error.NatsRemoteError] or the error of an undecodable answer. It also
     * rejects with [IllegalArgumentException] if [method] is not an `ALL_INSTANCES` method with a response message or
     * the request or options are invalid, and with [IllegalStateException] if the client is closed.
     */
    fun callAnswer(method: MethodSchema, request: ProtoMessageInit?, options: CallOptions? = null): Promise<Answer> {
        return apiScope.promise { client.callAnswer(method.asDesc(), request, options) }
    }

    /**
     * Closes the client and its connection.
     *
     * Stops accepting listeners, delivers the events the subscriptions already received and waits up to 10 seconds
     * for promises returned by handlers. Then it sends the buffered events and closes the connection. Handlers that
     * run while the client closes can still publish and call. A second call returns a promise that resolves when the
     * first close ends. A closed client cannot be reopened.
     *
     * @return a promise that resolves when the connection is closed
     */
    fun close(): Promise<Unit> = apiScope.promise { client.close() }
}
