// Typed facade over the Kotlin/JS exports (./@slne/surf-nats.mjs) for protobuf-es schemas.
import type {
    DescMessage,
    DescMethod,
    JsonObject,
    JsonValue,
    MessageInitShape,
    MessageShape,
} from "@bufbuild/protobuf";

export {
    NatsRpcError,
    NatsRemoteError,
    NatsNoRespondersError,
    NatsCallTimeoutError,
} from "./@slne/surf-nats.mjs";

/**
 * Connection settings. `name` is the own client name (kebab-case); it is used for `Surf-Origin`
 * (`<name>@<instanceId>`), the connection name and the queue group of `ANY_INSTANCE` listeners.
 *
 * At most one authentication method may be set: `token`, `user` with `pass`, or `credsFile` (path of a
 * `.creds` file). Empty strings count as unset. `servers` defaults to `nats://localhost:4222`; a string may
 * list several servers separated by commas.
 */
export interface ConnectOptions {
    readonly name: string;
    readonly servers?: string | readonly string[];
    readonly token?: string;
    readonly user?: string;
    readonly pass?: string;
    readonly credsFile?: string;
    readonly connectTimeoutMs?: number;
}

/**
 * `timeoutMs` bounds the flush that confirms the publish (default 5000). `eventId` identifies the logical event
 * (`Surf-Event-Id`, at most 128 visible ASCII characters, default a random UUID). Listeners that deduplicate by
 * event ID treat publishes with the same ID as one event.
 */
export interface PublishOptions {
    readonly timeoutMs?: number;
    readonly eventId?: string;
}

export type Recipients = "ANY_INSTANCE" | "ALL_INSTANCES";

/**
 * `ANY_INSTANCE` delivers each event to one listener per client name, `ALL_INSTANCES` (default) to every
 * listener. With `ignoreFromThisClient`, events published by this client are skipped; it requires
 * `ALL_INSTANCES` (`listen` rejects it with `ANY_INSTANCE`, where a skipped event would reach no other instance).
 */
export interface ListenOptions {
    readonly recipients?: Recipients;
    readonly ignoreFromThisClient?: boolean;
}

/**
 * `timeoutMs` overrides the call timeout of the contract (default 5 s, persistent 60 s).
 *
 * `idempotencyKey` (persistent methods only, 1 to 128 visible ASCII characters) identifies one logical action, such
 * as one submitted form. The responder receives it as the message ID (`Surf-Message-Id`), so repeating the call with
 * the same key and an equal request, for example after a timeout, reaches a responder that deduplicates by message
 * ID as the same operation and returns its stored reply, also within the two-minute duplicate window of JetStream.
 * Use a new key for every action; core calls ignore it.
 */
export interface CallOptions {
    readonly timeoutMs?: number;
    readonly idempotencyKey?: string;
}

/**
 * `eventId` is the publisher's `Surf-Event-Id`, which identifies the logical event, or null if the publisher sent
 * none.
 */
export interface EventContext {
    readonly origin?: string | null;
    readonly isFromThisClient: boolean;
    readonly eventId?: string | null;
}

export type Answer<T> =
    | { readonly known: true; readonly value: T }
    | { readonly known: false; readonly value?: undefined };

type IsEmpty<D extends DescMessage> = D["typeName"] extends "google.protobuf.Empty" ? true : false;

/**
 * `void` for methods returning `google.protobuf.Empty`, otherwise the response message.
 */
export type CallResult<M extends DescMethod> =
    IsEmpty<M["output"]> extends true ? void : MessageShape<M["output"]>;

/**
 * May return a promise; a rejected promise (or thenable) is logged like a thrown error. Other return values are
 * ignored, so expression bodies such as `(event) => list.push(event)` are accepted.
 */
export type EventHandler<D extends DescMessage> =
    (message: MessageShape<D>, context: EventContext) => unknown;

export interface NatsRegistration {
    readonly isClosed: boolean;
    close(): void;
}

/**
 * Operations on surf-nats contracts, addressed by the proto full names of their schemas.
 */
export interface SurfNatsOperations {
    /**
     * Publishes an event (a message with `option (surf.nats.v1.event) = true`). `required` fields are checked
     * before sending; an init object is converted with `create`.
     */
    publish<D extends DescMessage>(schema: D, message: MessageInitShape<D>, options?: PublishOptions): Promise<void>;

    /**
     * Subscribes to an event. Undecodable events and events that lack a `required` field are dropped with a warning.
     */
    listen<D extends DescMessage>(
        schema: D,
        handler: EventHandler<D>,
        options?: ListenOptions,
    ): Promise<NatsRegistration>;

    /**
     * Calls a method: core `ANY_INSTANCE`, `ALL_INSTANCES` without a result (fan-out), persistent fire and
     * persistent RPC. `ALL_INSTANCES` methods with a result are rejected; use `callAnswer`.
     */
    call<M extends DescMethod>(
        method: M,
        request: MessageInitShape<M["input"]>,
        options?: CallOptions,
    ): Promise<CallResult<M>>;

    /**
     * Calls an `ALL_INSTANCES` method with a result: the first known answer wins; without one the answer is
     * unknown, or the first recorded responder error is thrown.
     */
    callAnswer<M extends DescMethod>(
        method: M,
        request: MessageInitShape<M["input"]>,
        options?: CallOptions,
    ): Promise<Answer<MessageShape<M["output"]>>>;
}

export interface SurfNatsClient extends SurfNatsOperations {
    readonly name: string;
    readonly origin: string;
    readonly isClosed: boolean;

    /**
     * Drains the subscriptions and closes the connection.
     */
    close(): Promise<void>;
}

export declare function connect(options: ConnectOptions): Promise<SurfNatsClient>;

/**
 * A `surf.types.v1.Uuid` message: 16 bytes, the most significant half first (big-endian), as the JVM writes it.
 */
export interface UuidMessage {
    readonly $typeName: "surf.types.v1.Uuid";
    value: Uint8Array;
}

/**
 * Formats a `surf.types.v1.Uuid` (or its 16 bytes) as a lowercase `8-4-4-4-12` UUID string; throws a
 * `RangeError` for any other length.
 */
export declare function uuidToString(uuid: { readonly value: Uint8Array } | Uint8Array): string;

/**
 * Parses an `8-4-4-4-12` UUID string (any case) into a `surf.types.v1.Uuid` message; throws a `RangeError` for
 * other input.
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
 * `google.protobuf.Timestamp` becomes a `Date`, `surf.types.v1.Uuid` a UUID string and a 64-bit integer a `number`;
 * a 64-bit integer with `jstype = JS_STRING` stays a `string`. A `google/protobuf/wrappers.proto` type becomes its
 * value, `google.protobuf.Struct` a `JsonObject`, `Value` a `JsonValue` and `ListValue` a `JsonValue[]`. Messages lose
 * `$typeName` and `$unknown`. Fields with explicit presence (every proto2 singular field and every message field) are
 * typed `| null`, because protobuf-es types do not say which fields have presence.
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
 * `google.protobuf.Timestamp` becomes a `Date`, `surf.types.v1.Uuid` a UUID string and a 64-bit integer a `number`, or
 * a decimal `string` with `jstype = JS_STRING`. A `google/protobuf/wrappers.proto` type becomes its value, and
 * `google.protobuf.Struct`, `Value` and `ListValue` become JSON values; a `Value` of kind `null_value` becomes `null`.
 * A field with explicit presence is `null` while unset instead of its protobuf default; lists and maps are never
 * `null`. A oneof becomes `{ case, value }`, or `{ case: null, value: null }` while unset. Nested messages, lists and
 * maps are converted the same way and map keys become strings; enums stay numbers and bytes stay `Uint8Array`.
 * Throws a `RangeError` for a 64-bit integer outside ±(2^53 − 1), a timestamp outside the range of a `Date` or a UUID
 * without 16 bytes.
 */
export declare function toPlain<D extends DescMessage>(schema: D, message: MessageShape<D>): Plain<MessageShape<D>>;
