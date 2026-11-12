// Typed facade over the Kotlin/JS exports (./@slne/surf-nats.mjs) for protobuf-es schemas.

/**
 * Node.js client for surf-nats events and services, typed by protobuf-es schemas.
 *
 * Connect with {@link connect}, then publish and listen to events and call service methods through the returned
 * {@link SurfNatsClient}. Events and methods are addressed by the schemas that protoc-gen-es generates, so the client
 * needs no subject or other setting to reach them. The client logs connection changes, handler failures and dropped
 * events to the console.
 *
 * @example
 * ```ts
 * import { connect } from "@slne/surf-nats";
 * import { PlayerJoinedSchema } from "./gen/surf/game/v1/events_pb.js";
 * import { PlayerService } from "./gen/surf/game/v1/services_pb.js";
 *
 * const nats = await connect({ name: "panel", servers: "nats://localhost:4222" });
 *
 * const registration = await nats.listen(PlayerJoinedSchema, (event) => console.log(`${event.name} joined`));
 * nats.publish(PlayerJoinedSchema, { name: "Steve" });
 * const player = await nats.call(PlayerService.method.findPlayer, { name: "Steve" });
 *
 * registration.close();
 * await nats.close();
 * ```
 *
 * @packageDocumentation
 */
import type {
    DescMessage,
    DescMethod,
    JsonObject,
    JsonValue,
    MessageInitShape,
    MessageShape,
} from "@bufbuild/protobuf";

/*
 * Errors that `call` and `callAnswer` reject with. Each is a `NatsRpcError`:
 *
 * - `NatsRemoteError`: the service implementation threw. `type` is the name of the exception class and
 *   `remoteMessage` its message, the only details that cross the wire.
 * - `NatsNoRespondersError`: no instance serves the method. Only non-persistent `ANY_INSTANCE` calls reject with it.
 *   `subject` names the method's subject.
 * - `NatsCallTimeoutError`: no reply, or for a persistent method no storage confirmation from JetStream, arrived
 *   within the timeout. Carries `subject` and `timeoutMs`.
 * - `NatsRpcError` itself: JetStream rejected a persistent call, for example because its stream does not exist.
 *
 * Other failures, such as invalid arguments or options, an undecodable response or a closed client, are an `Error`
 * that is not a `NatsRpcError`. Model expected business failures in the response message, for example as a oneof,
 * and treat these errors as bugs or outages.
 */
export {
    NatsRpcError,
    NatsRemoteError,
    NatsNoRespondersError,
    NatsCallTimeoutError,
} from "./@slne/surf-nats.mjs";

/**
 * Settings for {@link connect}.
 *
 * Set at most one way to authenticate: `token`, `user` with `pass`, or `credsFile`. Empty strings count as unset.
 * The client reads no environment variables, so pass their values yourself.
 */
export interface ConnectOptions {
    /**
     * Your own client name in kebab-case: groups of lowercase letters and digits separated by `-`.
     *
     * The name identifies your application. The client sends `<name>@<instanceId>` as the origin of its messages and
     * as the connection name, and `ANY_INSTANCE` listeners of clients with the same name share the events.
     */
    readonly name: string;

    /**
     * The server URLs, as an array or as one comma-separated string. Blank entries are ignored.
     *
     * Defaults to `nats://localhost:4222`. A list without any URL is rejected.
     */
    readonly servers?: string | readonly string[];

    /**
     * The token for token authentication.
     */
    readonly token?: string;

    /**
     * The user name for user and password authentication. Requires {@link ConnectOptions.pass}.
     */
    readonly user?: string;

    /**
     * The password for user and password authentication. Requires {@link ConnectOptions.user}.
     */
    readonly pass?: string;

    /**
     * The path of a NATS `.creds` file, read once while connecting.
     */
    readonly credsFile?: string;

    /**
     * The timeout of the initial connection in milliseconds. Must be positive. Defaults to 5000.
     */
    readonly connectTimeoutMs?: number;
}

/**
 * Options for {@link SurfNatsOperations.publish}.
 */
export interface PublishOptions {
    /**
     * The ID of the logical event, sent as `Surf-Event-Id`: 1 to 128 visible ASCII characters, without spaces.
     *
     * Defaults to a random UUID. Listeners that deduplicate by event ID treat publishes with the same ID as one event,
     * so reuse the ID only when you publish the same event again.
     */
    readonly eventId?: string;
}

/**
 * Which listeners receive an event.
 *
 * - `ALL_INSTANCES`: every listener receives every event.
 * - `ANY_INSTANCE`: each event goes to one of the clients with the same client name, and every `ANY_INSTANCE`
 *   listener of that client for the event receives it. This spreads the events across the instances of one
 *   application.
 */
export type Recipients = "ANY_INSTANCE" | "ALL_INSTANCES";

/**
 * Options for {@link SurfNatsOperations.listen}.
 */
export interface ListenOptions {
    /**
     * Which listeners receive an event. Defaults to `ALL_INSTANCES`.
     */
    readonly recipients?: Recipients;

    /**
     * If true, skips the events this client published. If false or unset, the listener receives them too.
     *
     * Only events of this connection count as its own: another client, even one with the same name, is foreign.
     * Requires `recipients: "ALL_INSTANCES"`. `listen` rejects it with `ANY_INSTANCE`, where a skipped event would
     * reach no other instance.
     */
    readonly ignoreFromThisClient?: boolean;
}

/**
 * Options for {@link SurfNatsOperations.call} and {@link SurfNatsOperations.callAnswer}.
 */
export interface CallOptions {
    /**
     * The call timeout in milliseconds, overriding the contract's `call_timeout_seconds`. Must be positive.
     *
     * Defaults to the contract's timeout, or 5 seconds, or 60 seconds for a persistent method. A persistent method
     * without a result allows at most two minutes, the duplicate window of JetStream. Other calls have no upper
     * limit, unless the request exceeds the server's max payload: the object store keeps such a request for
     * 10 minutes, so its call rejects a longer timeout.
     */
    readonly timeoutMs?: number;

    /**
     * The key of one logical action, such as one submitted form: 1 to 128 visible ASCII characters.
     *
     * Only persistent methods use it. The service receives the key as the message ID of the call (`Surf-Message-Id`).
     * A service that deduplicates by message ID runs the action once and answers a repeated call with the same key
     * and an equal request, for example after a timeout, with its stored reply.
     * Use a new key for every action.
     */
    readonly idempotencyKey?: string;
}

/**
 * Metadata of a received event, passed to an {@link EventHandler}.
 */
export interface EventContext {
    /**
     * The publisher's origin, `<client name>@<instanceId>`, or null if the publisher sent none.
     */
    readonly origin?: string | null;

    /**
     * True if this connection published the event, false otherwise.
     */
    readonly isFromThisClient: boolean;

    /**
     * The publisher's `Surf-Event-Id`, which identifies the logical event, or null if the publisher sent none.
     *
     * Use it to deduplicate events that a publisher sent again.
     */
    readonly eventId?: string | null;
}

/**
 * The result of an `ALL_INSTANCES` method with a response message.
 *
 * `{ known: true, value }` holds the first known answer of an instance. `{ known: false }` means that no known
 * answer arrived: no instance serves the method, or none answered with a known value within the timeout.
 *
 * @example
 * ```ts
 * const located = await nats.callAnswer(PlayerService.method.locate, { name: "Steve" });
 * if (located.known) {
 *     console.log(`Steve is on ${located.value.server}`);
 * }
 * ```
 */
export type Answer<T> =
    | { readonly known: true; readonly value: T }
    | { readonly known: false; readonly value?: undefined };

type IsEmpty<D extends DescMessage> = D["typeName"] extends "google.protobuf.Empty" ? true : false;

/**
 * The result of {@link SurfNatsOperations.call}: `void` for a method that returns `google.protobuf.Empty`,
 * otherwise the response message.
 */
export type CallResult<M extends DescMethod> =
    IsEmpty<M["output"]> extends true ? void : MessageShape<M["output"]>;

/**
 * A listener for one event type, registered with {@link SurfNatsOperations.listen}.
 *
 * The client calls the handler once per received event with the decoded message and its {@link EventContext}. It
 * does not wait for a returned promise before it delivers the next event, so asynchronous handlers can overlap.
 * A thrown error or a rejected promise (or thenable) is logged to the console and does not affect other listeners.
 * Other return values are ignored, so expression bodies such as `(event) => list.push(event)` are accepted.
 */
export type EventHandler<D extends DescMessage> =
    (message: MessageShape<D>, context: EventContext) => unknown;

/**
 * The handle of a listener registered with {@link SurfNatsOperations.listen}.
 */
export interface NatsRegistration {
    /**
     * True after {@link NatsRegistration.close} or after the client closed, false otherwise.
     */
    readonly isClosed: boolean;

    /**
     * Stops the listener. It receives no further events.
     *
     * Returns immediately and does not wait for handlers that are still running. Calling it again has no effect.
     */
    close(): void;
}

/**
 * Operations on surf-nats events and service methods, addressed by their protobuf-es schemas.
 *
 * Events and services must be declared in a proto package that ends with a major version such as `.v1`. Their surf
 * options are read only when the declaring file imports `surf/nats/v1/options.proto` directly: without it a message
 * is no event and service options count as unset. Outgoing messages are checked before anything is sent: every
 * `required` field must be set, also in nested messages, and a `surf.types.v1.Uuid` must hold 16 bytes. Incoming
 * messages pass the same checks, and enum numbers that the schema does not know become the enum's zero value, or its
 * first value if it declares no zero.
 */
export interface SurfNatsOperations {
    /**
     * Publishes an event without waiting for the server.
     *
     * The connection buffers the event, and {@link SurfNatsClient.close} sends the buffered events before it ends.
     * Delivery is not guaranteed: a listener that is offline when the event arrives misses it. Events are never
     * moved to the object store, so an event above the server's max payload is rejected. Handlers that run while the
     * client closes can still publish.
     *
     * @param schema - The schema of the event: a message with `option (surf.nats.v1.event) = true`.
     * @param message - The event, as a message or as an init object that the client converts with `create`.
     * @param options - The event ID. See {@link PublishOptions}.
     * @throws Error if the schema is no event, the message is invalid or not of the schema, the event ID is
     *   invalid, the event exceeds the server's max payload, or the client is closed. Nothing is sent then.
     *
     * @example
     * ```ts
     * nats.publish(PlayerJoinedSchema, { name: "Steve" });
     * nats.publish(ServerStartedSchema, { server: "lobby-1" }, { eventId: "lobby-1-started" });
     * ```
     */
    publish<D extends DescMessage>(schema: D, message: MessageInitShape<D>, options?: PublishOptions): void;

    /**
     * Registers a listener for an event.
     *
     * The promise resolves once the server knows the subscription, so the listener receives every event published
     * after that. Events that cannot be decoded, that have no protobuf content type or that lack a `required` field
     * are dropped with a warning.
     *
     * @param schema - The schema of the event: a message with `option (surf.nats.v1.event) = true`.
     * @param handler - The function that receives each event. See {@link EventHandler}.
     * @param options - Which listeners receive an event and whether to skip own events. See {@link ListenOptions}.
     * @returns The registration that stops the listener.
     * @throws Error (as a rejected promise) if the schema is no event, the options are invalid, another schema object
     *   for an event with the same full name already has a listener on this client, or the client is closing or
     *   closed.
     *
     * @example
     * ```ts
     * const registration = await nats.listen(PlayerJoinedSchema, (event, context) => {
     *     console.log(`${event.name} joined, published by ${context.origin}`);
     * }, { recipients: "ANY_INSTANCE" });
     *
     * // To stop listening:
     * registration.close();
     * ```
     */
    listen<D extends DescMessage>(
        schema: D,
        handler: EventHandler<D>,
        options?: ListenOptions,
    ): Promise<NatsRegistration>;

    /**
     * Calls a service method and resolves with its response.
     *
     * Handles every unary method except `ALL_INSTANCES` methods with a response message, which need
     * {@link SurfNatsOperations.callAnswer}. What the promise waits for depends on the method:
     *
     * - `ANY_INSTANCE`: one instance handles the request. The promise resolves with its response, or after the
     *   handler completed for a method that returns `google.protobuf.Empty`.
     * - `ALL_INSTANCES` returning `google.protobuf.Empty`: every instance receives the request. The promise resolves
     *   once the connection has buffered the request, without waiting for the server, and reports no failure of an
     *   instance.
     * - Persistent, returning `google.protobuf.Empty`: the promise resolves once JetStream stored the request. The
     *   client sends it again with the same `Nats-Msg-Id` until JetStream confirms it, and the service processes it
     *   later.
     * - Persistent, with a response message: JetStream stores the request and the promise resolves with the
     *   response of the instance that handled it.
     *
     * Requests and responses above the server's max payload go through the object store, which requires JetStream.
     * The client creates the missing object store buckets, but never a stream. Except for persistent methods
     * without a result, a request carries the absolute time at which the call expires, so the hosts need
     * synchronized clocks.
     *
     * @param method - The method descriptor that protoc-gen-es generates, such as `PlayerService.method.findPlayer`.
     * @param request - The request, as a message or as an init object that the client converts with `create`.
     * @param options - The timeout and the idempotency key. See {@link CallOptions}.
     * @returns The response message, or `undefined` for a method that returns `google.protobuf.Empty`.
     * @throws NatsRemoteError if the service implementation threw.
     * @throws NatsNoRespondersError if no instance serves a non-persistent `ANY_INSTANCE` method.
     * @throws NatsCallTimeoutError if no response, or no storage confirmation from JetStream, arrived in time.
     * @throws NatsRpcError if JetStream rejected a persistent request, for example because its stream does not exist.
     * @throws Error if the method is invalid or needs `callAnswer`, the request or options are invalid, a response
     *   cannot be decoded, or the client is closed. Every error arrives as a rejected promise.
     *
     * @example
     * ```ts
     * const player = await nats.call(PlayerService.method.findPlayer, { name: "Steve" });
     * await nats.call(PlayerService.method.kick, { name: "Steve" }, { timeoutMs: 2000 });
     *
     * try {
     *     await nats.call(PlayerService.method.findPlayer, { name: "Alex" });
     * } catch (e) {
     *     if (e instanceof NatsRemoteError) console.error(`${e.type}: ${e.remoteMessage}`);
     *     else throw e;
     * }
     * ```
     */
    call<M extends DescMethod>(
        method: M,
        request: MessageInitShape<M["input"]>,
        options?: CallOptions,
    ): Promise<CallResult<M>>;

    /**
     * Calls an `ALL_INSTANCES` method with a response message on every instance and resolves with the first known
     * answer.
     *
     * An instance answers with a value or marks its answer as unknown. The client does not track which instances
     * are running, so without a known answer the promise resolves only after the timeout, unless no instance serves
     * the method. Choose a short `timeoutMs` for methods that often have no known answer.
     *
     * @param method - The method descriptor that protoc-gen-es generates, such as `PlayerService.method.locate`.
     * @param request - The request, as a message or as an init object that the client converts with `create`.
     * @param options - The timeout. See {@link CallOptions}.
     * @returns `{ known: true, value }` with the first known answer, or `{ known: false }` if none arrived in time.
     * @throws NatsRemoteError if no known answer arrived and an instance threw. With several failures, the first
     *   one is thrown. A timeout never rejects with `NatsCallTimeoutError`.
     * @throws Error if the method is not an `ALL_INSTANCES` method with a response message, the request or options
     *   are invalid, no known answer arrived and an answer could not be decoded, or the client is closed. Every
     *   error arrives as a rejected promise.
     *
     * @example
     * ```ts
     * const located = await nats.callAnswer(PlayerService.method.locate, { name: "Steve" }, { timeoutMs: 1000 });
     * console.log(located.known ? `on ${located.value.server}` : "offline");
     * ```
     */
    callAnswer<M extends DescMethod>(
        method: M,
        request: MessageInitShape<M["input"]>,
        options?: CallOptions,
    ): Promise<Answer<MessageShape<M["output"]>>>;
}

/**
 * A connected surf-nats client, created by {@link connect}.
 *
 * Each client owns one NATS connection. After a connection loss it reconnects without a limit on attempts.
 */
export interface SurfNatsClient extends SurfNatsOperations {
    /**
     * The client name from {@link ConnectOptions.name}.
     */
    readonly name: string;

    /**
     * The origin this client sends with its messages: `<name>@<instanceId>`, with a random instance ID per
     * {@link connect}.
     */
    readonly origin: string;

    /**
     * True once {@link SurfNatsClient.close} has completed, false while the client is open or closing.
     */
    readonly isClosed: boolean;

    /**
     * Closes the client and its connection.
     *
     * Stops accepting listeners, delivers the events the subscriptions already received, and waits up to
     * 10 seconds for promises returned by handlers. Then it sends the buffered events and closes the connection.
     * Handlers that run while the client closes can still publish and call. Calling `close()` again returns a
     * promise that resolves when the first close ends. A closed client cannot be reopened, so call {@link connect}
     * again.
     *
     * @returns A promise that resolves when the connection is closed.
     */
    close(): Promise<void>;
}

/**
 * Connects a new client to NATS.
 *
 * Every call creates an independent client with its own connection and instance ID.
 *
 * @param options - The client name, servers and authentication. See {@link ConnectOptions}.
 * @returns The connected client.
 * @throws Error (as a rejected promise) if the name is not kebab-case, `servers` holds no URL, the authentication
 *   settings conflict, `connectTimeoutMs` is not positive, the `credsFile` cannot be read, or the connection fails.
 *
 * @example
 * ```ts
 * import { connect } from "@slne/surf-nats";
 *
 * const nats = await connect({ name: "panel", servers: "nats://localhost:4222", token: "change-me" });
 *
 * // On shutdown:
 * await nats.close();
 * ```
 */
export declare function connect(options: ConnectOptions): Promise<SurfNatsClient>;

/**
 * A `surf.types.v1.Uuid` message: 16 bytes, the most significant half first (big-endian), as the JVM writes it.
 */
export interface UuidMessage {
    readonly $typeName: "surf.types.v1.Uuid";
    value: Uint8Array;
}

/**
 * Formats a `surf.types.v1.Uuid` as a lowercase `8-4-4-4-12` UUID string.
 *
 * @param uuid - The message or its 16 bytes.
 * @returns The UUID string.
 * @throws RangeError if the value does not hold exactly 16 bytes.
 *
 * @example
 * ```ts
 * if (event.playerId) {
 *     const id: string = uuidToString(event.playerId); // "0b6f1c2e-6d4f-4d1a-9f0e-2c3b1a7e5d90"
 * }
 * ```
 */
export declare function uuidToString(uuid: { readonly value: Uint8Array } | Uint8Array): string;

/**
 * Parses a UUID string into a `surf.types.v1.Uuid` message.
 *
 * @param value - A UUID in `8-4-4-4-12` form, in any case.
 * @returns The message with the 16 bytes of the UUID.
 * @throws RangeError if the value is not an `8-4-4-4-12` hexadecimal string.
 *
 * @example
 * ```ts
 * nats.publish(PlayerJoinedSchema, { name: "Alex", id: uuidFromString("0b6f1c2e-6d4f-4d1a-9f0e-2c3b1a7e5d90") });
 * ```
 */
export declare function uuidFromString(value: string): UuidMessage;

type PlainOneof<T> =
    T extends { readonly case: undefined } ? { readonly case: null; readonly value: null }
    : T extends { readonly case: infer C; readonly value: infer V } ? { readonly case: C; readonly value: Plain<V> }
    : never;

/**
 * The plain value of each `google/protobuf/wrappers.proto` type, by type name.
 */
type PlainWrappers = {
    "google.protobuf.DoubleValue": number;
    "google.protobuf.FloatValue": number;
    "google.protobuf.Int64Value": number;
    "google.protobuf.UInt64Value": number;
    "google.protobuf.Int32Value": number;
    "google.protobuf.UInt32Value": number;
    "google.protobuf.BoolValue": boolean;
    "google.protobuf.StringValue": string;
    "google.protobuf.BytesValue": Uint8Array;
};

/**
 * True for exactly `JsonObject`, the protobuf-es type of a `google.protobuf.Struct` field, and false for a map type
 * that is assignable to it.
 */
type IsJsonObject<T> = [T] extends [JsonObject] ? ([JsonObject] extends [T] ? true : false) : false;

/**
 * The plain form of a field of a protobuf-es message: lists and maps are never null, oneofs become
 * {@link PlainOneof}, and every other field is null while unset.
 */
type PlainField<T> =
    T extends readonly unknown[] ? Plain<T>
    : T extends { readonly $typeName: string } ? Plain<T> | null
    : T extends { readonly case: unknown } ? PlainOneof<T>
    : T extends Uint8Array ? T | null
    : IsJsonObject<T> extends true ? JsonObject | null
    : T extends object ? Plain<T>
    : Plain<T> | null;

/**
 * The shape {@link toPlain} gives a protobuf-es message or field value.
 *
 * `google.protobuf.Timestamp` becomes a `Date`, `surf.types.v1.Uuid` a UUID string and a 64-bit integer a `number`.
 * A 64-bit integer with `jstype = JS_STRING` stays a `string`. A `google/protobuf/wrappers.proto` type becomes its
 * value, `google.protobuf.Struct` a `JsonObject`, `Value` a `JsonValue` and `ListValue` a `JsonValue[]`. Messages lose
 * `$typeName` and `$unknown`. Every singular field outside a oneof is typed `| null`, because protobuf-es types do not
 * say which fields have explicit presence, and lists and maps are never null.
 */
export type Plain<T> =
    T extends { readonly $typeName: "google.protobuf.Timestamp" } ? Date
    : T extends { readonly $typeName: "surf.types.v1.Uuid" } ? string
    : T extends { readonly $typeName: infer N extends keyof PlainWrappers } ? PlainWrappers[N]
    : T extends { readonly $typeName: "google.protobuf.Struct" } ? JsonObject
    : T extends { readonly $typeName: "google.protobuf.Value" } ? JsonValue
    : T extends { readonly $typeName: "google.protobuf.ListValue" } ? JsonValue[]
    : T extends bigint ? number
    : T extends Uint8Array ? T
    : T extends readonly (infer E)[] ? Plain<E>[]
    : T extends { readonly $typeName: string }
        ? { -readonly [K in keyof T as K extends "$typeName" | "$unknown" ? never : K]-?: PlainField<T[K]> }
    : IsJsonObject<T> extends true ? JsonObject
    : T extends object ? { -readonly [K in keyof T]: Plain<T[K]> }
    : T;

/**
 * Converts a protobuf-es message into plain data that JSON and superjson can carry.
 *
 * Use it to pass messages to code that expects plain objects, such as a JSON column or a superjson response.
 *
 * - `google.protobuf.Timestamp` becomes a `Date`, `surf.types.v1.Uuid` a UUID string and a 64-bit integer a
 *   `number`, or a decimal `string` with `jstype = JS_STRING`.
 * - A `google/protobuf/wrappers.proto` type becomes its value. `google.protobuf.Struct`, `Value` and `ListValue`
 *   become JSON values, and a `Value` of kind `null_value` or without a kind becomes `null`.
 * - A field with explicit presence, such as a proto2 singular field or a message field, is `null` while unset
 *   instead of its protobuf default. Lists and maps are never `null`.
 * - A oneof becomes `{ case, value }` with the field's local name as `case`, or `{ case: null, value: null }` while
 *   unset.
 * - Nested messages, lists and maps are converted the same way, and map keys become strings. Enums stay numbers,
 *   bytes stay `Uint8Array`, and messages lose `$typeName` and `$unknown`.
 *
 * @param schema - The schema of the message.
 * @param message - A message of `schema`.
 * @returns The plain data, typed as {@link Plain}.
 * @throws RangeError if a 64-bit integer is outside ±(2^53 − 1), a timestamp is outside the range of a `Date`, or a
 *   UUID does not hold 16 bytes.
 * @throws Error if `message` is not a message of `schema`.
 *
 * @example
 * ```ts
 * const response = await nats.call(PlayerService.method.findPlayer, { name: "Steve" });
 * const plain = toPlain(FindPlayerResponseSchema, response); // { name: "Steve", server: null }
 * ```
 */
export declare function toPlain<D extends DescMessage>(schema: D, message: MessageShape<D>): Plain<MessageShape<D>>;
