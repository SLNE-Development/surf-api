// tsc --strict check of a consumer of the @slne/surf-nats npm distribution (checkJsConsumerTypes).
import {
    connect,
    NatsCallTimeoutError,
    NatsNoRespondersError,
    NatsRemoteError,
    NatsRpcError,
    type Answer,
    type EventContext,
    type NatsRegistration,
    type Plain,
    type SurfNatsClient,
    toPlain,
    type UuidMessage,
    uuidFromString,
    uuidToString,
} from "@slne/surf-nats";
import type { Uuid } from "./gen/surf/types/v1/uuid_pb.js";
import { create, type JsonObject, type JsonValue } from "@bufbuild/protobuf";
import { ValueSchema } from "@bufbuild/protobuf/wkt";
import { ChatMessageSchema, PlayerJoinedSchema, ServerStartedSchema } from "./gen/surf/natsjs/test/v1/events_pb.js";
import {
    PlainSampleSchema,
    PlainWellKnownSchema,
    type PlainSample,
    type PlainWellKnown,
} from "./gen/surf/natsjs/test/v1/plain_pb.js";
import {
    AuditService,
    PlayerService,
    type FindPlayerResponse,
    type LocateResponse,
} from "./gen/surf/natsjs/test/v1/services_pb.js";

declare function log(...values: unknown[]): void;

export async function consume(): Promise<void> {
    const nats: SurfNatsClient = await connect({ name: "surf-panel", servers: ["nats://localhost:4222"], token: "t" });

    nats.publish(ChatMessageSchema, { text: "hi" });
    nats.publish(PlayerJoinedSchema, create(PlayerJoinedSchema, { name: "Steve", id: { value: new Uint8Array(16) } }));
    const playerId: Uuid = uuidFromString("00112233-4455-6677-8899-aabbccddeeff");
    const helperId: UuidMessage = uuidFromString("00112233-4455-6677-8899-aabbccddeeff");
    nats.publish(PlayerJoinedSchema, { name: "Alex", id: playerId });
    const joinedMessage = create(PlayerJoinedSchema, { name: "Alex" });
    joinedMessage.id = helperId;
    const playerIdText: string = uuidToString(playerId) + uuidToString(helperId.value);
    nats.publish(ServerStartedSchema, { server: "lobby-1" }, { eventId: "lobby-1-started" });
    nats.publish(ServerStartedSchema, { server: "lobby-1" }, { eventId: "custom-event-1" });
    const published: void = nats.publish(ChatMessageSchema, { text: "hi" });
    log(published);

    const registration: NatsRegistration = await nats.listen(
        PlayerJoinedSchema,
        (event, context: EventContext) => {
            const name: string = event.name;
            const server: string = event.server;
            const fromSelf: boolean = context.isFromThisClient;
            const eventId: string | null | undefined = context.eventId;
            log(name, server, fromSelf, context.origin, eventId);
        },
        { recipients: "ALL_INSTANCES", ignoreFromThisClient: true },
    );
    registration.close();

    const joined: unknown[] = [];
    await nats.listen(PlayerJoinedSchema, (event) => joined.push(event));
    await nats.listen(PlayerJoinedSchema, async (event) => {
        joined.push(event);
    });
    await nats.listen(ChatMessageSchema, (event) => Promise.resolve(event.text));
    await connect({ name: "surf-panel", servers: "nats://a:4222,nats://b:4222" });

    const found: FindPlayerResponse = await nats.call(PlayerService.method.findPlayer, { name: "Steve" });
    const kicked: void = await nats.call(PlayerService.method.kick, { name: "Steve" }, { timeoutMs: 2000 });
    const located: Answer<LocateResponse> = await nats.callAnswer(PlayerService.method.locate, { name: "Steve" });
    if (located.known) {
        const server: string = located.value.server;
        log(server);
    }
    await nats.call(AuditService.method.record, { action: "ban" });
    const count: number = (await nats.call(AuditService.method.query, { action: "ban" }, { idempotencyKey: "ban-42" })).count;
    log(found.name, kicked, count, playerIdText);

    try {
        await nats.call(PlayerService.method.findPlayer, {});
    } catch (e) {
        if (e instanceof NatsRemoteError) log(e.type, e.remoteMessage);
        if (e instanceof NatsNoRespondersError) log(e.subject);
        if (e instanceof NatsCallTimeoutError) log(e.subject, e.timeoutMs);
        if (e instanceof NatsRpcError) log(e.message);
    }

    const plain: Plain<PlainSample> = toPlain(PlainSampleSchema, create(PlainSampleSchema, { name: "Steve" }));
    const plainName: string | null = plain.name;
    const balance: number | null = plain.balance;
    const createdAt: Date | null = plain.createdAt;
    const plainId: string | null = plain.id;
    const history: number[] = plain.history;
    const scores: { [key: string]: number } = plain.scores;
    const parentNote: string | null | undefined = plain.nested?.parent?.note;
    if (plain.target.case === "group") {
        const groupNote: string | null = plain.target.value.note;
        log(groupNote);
    } else if (plain.target.case === "ticket") {
        const ticket: number = plain.target.value;
        log(ticket);
    } else if (plain.target.case === null) {
        const none: null = plain.target.value;
        log(none);
    }
    log(plainName, balance, createdAt, plainId, history, scores, parentNote);

    const wellKnown: Plain<PlainWellKnown> = toPlain(PlainWellKnownSchema, create(PlainWellKnownSchema));
    const label: string | null = wellKnown.label;
    const total: number | null = wellKnown.count;
    const flag: boolean | null = wellKnown.flag;
    const blob: Uint8Array | null = wellKnown.blob;
    const tags: string[] = wellKnown.tags;
    const limits: { [key: string]: number } = wellKnown.limits;
    const attributes: JsonObject | null = wellKnown.attributes;
    const setting: JsonValue = wellKnown.setting;
    const entries: JsonValue[] | null = wellKnown.entries;
    const documents: JsonObject[] = wellKnown.documents;
    const options: { [key: string]: JsonValue } = wellKnown.options;
    const bigId: string | null = wellKnown.bigId;
    const bigIds: string[] = wellKnown.bigIds;
    if (wellKnown.choice.case === "wrapped") {
        const wrapped: string = wellKnown.choice.value;
        log(wrapped);
    } else if (wellKnown.choice.case === "document") {
        const document: JsonObject = wellKnown.choice.value;
        log(document);
    }
    const value: JsonValue = toPlain(ValueSchema, create(ValueSchema));
    log(label, total, flag, blob, tags, limits, attributes, setting, entries, documents, options, bigId, bigIds, value);

    await nats.close();
}

export async function rejected(nats: SurfNatsClient): Promise<void> {
    // @ts-expect-error a plain object is no schema
    nats.publish({ nope: 1 }, {});
    // @ts-expect-error unknown field of the event
    nats.publish(ChatMessageSchema, { nope: 1 });
    // @ts-expect-error publish is fire and forget and has no timeout
    nats.publish(ChatMessageSchema, { text: "x" }, { timeoutMs: 1000 });
    // @ts-expect-error publish returns void, not a promise
    const pending: Promise<void> = nats.publish(ChatMessageSchema, { text: "x" });
    // @ts-expect-error a request of another method
    await nats.call(PlayerService.method.locate, { action: "x" });
    // @ts-expect-error the result of a call is the response message, not an Answer
    const answer: Answer<FindPlayerResponse> = await nats.call(PlayerService.method.findPlayer, { name: "x" });
    // @ts-expect-error unknown recipients
    await nats.listen(ChatMessageSchema, () => {}, { recipients: "SOME_INSTANCES" });
    // @ts-expect-error a handler for another message type
    await nats.listen(ChatMessageSchema, (event: { server: string }) => event.server);
    // @ts-expect-error servers must be strings
    await connect({ name: "surf-panel", servers: [4222] });
    // @ts-expect-error a UUID is formatted from a Uuid message or its bytes
    uuidToString("00112233-4455-6677-8899-aabbccddeeff");
    // @ts-expect-error contracts are addressed by their schema, not by an owning client
    nats.target("surf-punish");
    // @ts-expect-error an idempotency key is a string
    await nats.call(AuditService.method.record, { action: "ban" }, { idempotencyKey: 42 });
    // @ts-expect-error the options name no owner
    await nats.call(AuditService.method.record, { action: "ban" }, { owner: "surf-punish" });
    const plain = toPlain(PlainSampleSchema, create(PlainSampleSchema));
    // @ts-expect-error toPlain drops $typeName
    log(plain.$typeName);
    // @ts-expect-error an unset field is null, so the plain value is not a plain string
    const nickname: string = plain.nickname;
    // @ts-expect-error a 64-bit integer becomes a number
    const balance: bigint | null = plain.balance;
    const wellKnown = toPlain(PlainWellKnownSchema, create(PlainWellKnownSchema));
    // @ts-expect-error a wrapper becomes its value, not a message
    const tags: { value: string }[] = wellKnown.tags;
    // @ts-expect-error an unset Struct field is null
    const attributes: JsonObject = wellKnown.attributes;
    // @ts-expect-error a 64-bit integer with jstype = JS_STRING stays a string
    const bigId: number | null = wellKnown.bigId;
    log(answer, nickname, balance, tags, attributes, bigId);
}
