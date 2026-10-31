// tsc --strict check of the clients protoc-gen-surf-nats generates (target=js+dts) for the JS test protos.
import { connect, type Answer, type CallOptions, type SurfNatsOperations } from "@slne/surf-nats";
import type { FindPlayerResponse, LocateResponse } from "./gen/surf/natsjs/test/v1/services_pb.js";
import {
    createAuditServiceClient,
    createPlayerServiceClient,
    type PlayerServiceClient,
} from "./gen/surf/natsjs/test/v1/services_nats.js";
import { createLookupServiceClient } from "./gen/surf/natsjs/lookup/v1/lookup_nats.js";
import type { FindPlayerResponse as LookupResponse, ResolveResponse } from "./gen/surf/natsjs/lookup/v1/lookup_pb.js";

declare function log(...values: unknown[]): void;

export async function useClients(): Promise<void> {
    const nats = await connect({ name: "surf-panel" });
    const players: PlayerServiceClient = createPlayerServiceClient(nats);

    const found: FindPlayerResponse = await players.findPlayer({ name: "Steve" });
    const kicked: void = await players.kick({ name: "Steve" }, { timeoutMs: 2000 });
    const located: Answer<LocateResponse> = await players.locate({ name: "Steve" });
    const broadcast: void = await players.broadcast({ name: "Steve" });
    if (located.known) log(located.value.server);

    const audit = createAuditServiceClient(nats);
    const recorded: void = await audit.record({ action: "ban" });
    const count: number = (await audit.query({ action: "ban" })).count;
    const pinged: void = await audit.ping({});

    const lookup = createLookupServiceClient(nats);
    const resolved: ResolveResponse = await lookup.resolve({ name: "Steve" });
    const own: LookupResponse = await lookup.find({ name: "Steve" });
    const original: FindPlayerResponse = await lookup.findOriginal({ name: "Steve" });
    log(found.name, kicked, broadcast, recorded, count, pinged, resolved.id?.value, own.name, original.server);
}

// A hand-written fake satisfies the generated interface.
export const fakePlayers: PlayerServiceClient = {
    findPlayer: async (_request, _options?: CallOptions) => {
        throw new Error("offline");
    },
    kick: async () => {},
    locate: async () => ({ known: false }),
    broadcast: async () => {},
    slowFind: async () => {
        throw new Error("slow");
    },
    echo: async () => {
        throw new Error("echo");
    },
};

export async function rejectedClients(nats: SurfNatsOperations): Promise<void> {
    const players = createPlayerServiceClient(nats);
    // @ts-expect-error unknown request field
    await players.findPlayer({ nope: 1 });
    // @ts-expect-error an Answer is not the response message
    const response: FindPlayerResponse = await players.locate({ name: "x" });
    // @ts-expect-error an Empty response resolves to void
    const kicked: FindPlayerResponse = await players.kick({ name: "x" });
    // @ts-expect-error the options name no owner
    await players.findPlayer({ name: "x" }, { owner: "surf-punish" });
    log(response, kicked);
}
