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
    type SurfNatsClient,
    type UuidMessage,
    uuidFromString,
    uuidToString,
} from "@slne/surf-nats";
import type { Uuid } from "./gen/surf/types/v1/uuid_pb.js";
import { create } from "@bufbuild/protobuf";
import { ChatMessageSchema, PlayerJoinedSchema, ServerStartedSchema } from "./gen/surf/natsjs/test/v1/events_pb.js";
import {
    AuditService,
    PlayerService,
    type FindPlayerResponse,
    type LocateResponse,
} from "./gen/surf/natsjs/test/v1/services_pb.js";

declare function log(...values: unknown[]): void;

export async function consume(): Promise<void> {
    const nats: SurfNatsClient = await connect({ name: "surf-panel", servers: ["nats://localhost:4222"], token: "t" });

    await nats.publish(ChatMessageSchema, { text: "hi" });
    await nats.publish(PlayerJoinedSchema, create(PlayerJoinedSchema, { name: "Steve", id: { value: new Uint8Array(16) } }));
    const playerId: Uuid = uuidFromString("00112233-4455-6677-8899-aabbccddeeff");
    const helperId: UuidMessage = uuidFromString("00112233-4455-6677-8899-aabbccddeeff");
    await nats.publish(PlayerJoinedSchema, { name: "Alex", id: playerId });
    const joinedMessage = create(PlayerJoinedSchema, { name: "Alex" });
    joinedMessage.id = helperId;
    const playerIdText: string = uuidToString(playerId) + uuidToString(helperId.value);
    await nats.publish(ServerStartedSchema, { server: "lobby-1" }, { timeoutMs: 1000 });

    const registration: NatsRegistration = await nats.listen(
        PlayerJoinedSchema,
        (event, context: EventContext) => {
            const name: string = event.name;
            const server: string = event.server;
            const fromSelf: boolean = context.isFromThisClient;
            log(name, server, fromSelf, context.origin);
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
    const count: number = (await nats.call(AuditService.method.query, { action: "ban" })).count;
    log(found.name, kicked, count, playerIdText);

    try {
        await nats.call(PlayerService.method.findPlayer, {});
    } catch (e) {
        if (e instanceof NatsRemoteError) log(e.type, e.remoteMessage);
        if (e instanceof NatsNoRespondersError) log(e.subject);
        if (e instanceof NatsCallTimeoutError) log(e.subject, e.timeoutMs);
        if (e instanceof NatsRpcError) log(e.message);
    }

    await nats.close();
}

export async function rejected(nats: SurfNatsClient): Promise<void> {
    // @ts-expect-error a plain object is no schema
    await nats.publish({ nope: 1 }, {});
    // @ts-expect-error unknown field of the event
    await nats.publish(ChatMessageSchema, { nope: 1 });
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
    // @ts-expect-error the options name no owner
    await nats.call(AuditService.method.record, { action: "ban" }, { owner: "surf-punish" });
    log(answer);
}
