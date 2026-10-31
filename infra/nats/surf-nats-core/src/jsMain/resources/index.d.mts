// Typed facade over the Kotlin/JS exports (./@slne/surf-nats.mjs) for protobuf-es schemas.
import type { DescMessage, DescMethod, MessageInitShape, MessageShape } from "@bufbuild/protobuf";

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
 * `timeoutMs` bounds the flush that confirms the publish (default 5000).
 */
export interface PublishOptions {
    readonly timeoutMs?: number;
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
 */
export interface CallOptions {
    readonly timeoutMs?: number;
}

export interface EventContext {
    readonly origin?: string | null;
    readonly isFromThisClient: boolean;
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
     * Publishes an event (a message with `option (surf.nats.v1.event) = true`). Required fields are checked
     * before sending; an init object is converted with `create`.
     */
    publish<D extends DescMessage>(schema: D, message: MessageInitShape<D>, options?: PublishOptions): Promise<void>;

    /**
     * Subscribes to an event. Undecodable events and events without a required field are dropped with a warning.
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
