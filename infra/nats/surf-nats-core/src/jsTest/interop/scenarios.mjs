// Flow scenarios of the interop script; each one matches a test of JsInteropTest and reports the checks it expects.
import assert from "node:assert/strict";
import { isFieldSet } from "@bufbuild/protobuf";
import {
    NatsCallTimeoutError,
    NatsNoRespondersError,
    NatsRemoteError,
    NatsRpcError,
} from "@slne/surf-nats";
import { BlobService } from "./gen/surf/nats/test/v1/blobs_pb.js";
import { CalcService } from "./gen/surf/nats/test/v1/calc_pb.js";
import { NumberSchema, TextSchema } from "./gen/surf/nats/test/v1/common_pb.js";
import { OriginEventSchema, PanelEventSchema } from "./gen/surf/nats/test/v1/events_pb.js";
import { FinderService, FindReplySchema } from "./gen/surf/nats/test/v1/finder_pb.js";
import { InteropEventSchema } from "./gen/surf/nats/test/v1/interop_pb.js";
import { LookupReplySchema, OrderService } from "./gen/surf/nats/test/v1/orders_pb.js";
import { RolesService } from "./gen/surf/nats/test/v1/roles_pb.js";
import {
    VersionedPingReplySchema as V1PingReplySchema,
    VersionedService as V1VersionedService,
} from "./gen/surf/nats/test/v1/versioned_pb.js";
import {
    VersionedPingReplySchema as V2PingReplySchema,
    VersionedService as V2VersionedService,
} from "./gen/surf/nats/test/v2/versioned_pb.js";
import { assertMessage, blob, LARGE, passed, ready, SETTLE_MS, sleep, until, UUID_BYTES } from "./support.mjs";
import { runVectors } from "./vectors.mjs";

// Mirrors JsInteropTest.EVENT
const EVENT = {
    scalars: {
        unsignedSmall: 2147483648,
        unsignedLarge: 18446744073709551615n,
        signedLarge: -5n,
        small: -7,
        large: 9223372036854775807n,
        fixedSmall: 4294967295,
        ratio: 0.5,
        flag: true,
    },
    types: {
        uuid: { value: UUID_BYTES },
        timestamp: { seconds: 1700000000n, nanos: 123456789 },
        duration: { seconds: -1n, nanos: -500000000 },
    },
};

const LARGE_CALL = { timeoutMs: 30_000 };

function assertBlob(actual, label) {
    assert.ok(Buffer.from(actual).equals(Buffer.from(blob(LARGE))), `${label}: ${actual.length} bytes, content differs`);
}

export const scenarios = {
    async "publish-events"(nats, { owner }) {
        await nats.target(owner).publish(InteropEventSchema, EVENT);
        passed("prefixed");
        // Global contract: no owner
        await nats.publish(OriginEventSchema, { id: 7n });
        passed("global");
    },

    async "listen-events"(nats, { owner }) {
        const all = [];
        const any = [];
        const global = [];
        const jvm = nats.target(owner);
        await jvm.listen(InteropEventSchema, (event) => all.push(event));
        await jvm.listen(InteropEventSchema, (event) => any.push(event), { recipients: "ANY_INSTANCE" });
        await nats.listen(PanelEventSchema, (event) => global.push(event));
        ready("listening");

        await until(() => all.length > 0 && any.length > 0 && global.length > 0, "the JVM events");
        await sleep(SETTLE_MS);
        assert.equal(all.length, 1, "ALL_INSTANCES deliveries");
        assertMessage(InteropEventSchema, all[0], EVENT, "ALL_INSTANCES listener");
        passed("all");
        assert.equal(any.length, 1, "ANY_INSTANCE deliveries");
        assertMessage(InteropEventSchema, any[0], EVENT, "ANY_INSTANCE listener");
        passed("any");
        assert.equal(global.length, 1, "global deliveries");
        assertMessage(PanelEventSchema, global[0], { id: 9n }, "global listener");
        passed("global");
    },

    async "call-any"(nats, { owner }) {
        const jvm = nats.target(owner);
        const text = await jvm.call(CalcService.method.mixed, {
            a: -3,
            b: 9007199254740993n,
            factor: 0.5,
            flag: true,
            label: "surf",
        });
        assertMessage(TextSchema, text, { value: "-3|9007199254740993|0.5|true|surf" }, "mixed");
        passed("value");
        assert.equal(await jvm.call(CalcService.method.fire, {}), undefined);
        passed("unit");
    },

    async "call-errors"(nats, { owner }) {
        const jvm = nats.target(owner);
        await assert.rejects(async () => jvm.call(CalcService.method.fail, { message: "boom" }), (e) => {
            assert.ok(e instanceof NatsRemoteError, `NatsRemoteError expected, got ${e}`);
            assert.ok(e instanceof NatsRpcError);
            assert.equal(e.type, "java.lang.IllegalStateException");
            assert.equal(e.remoteMessage, "boom");
            return true;
        });
        passed("remote-error");

        const nobody = "interop-nobody";
        const request = { a: 1, b: 2n, factor: 1, flag: false, label: "x" };
        await assert.rejects(async () => nats.call(CalcService.method.mixed, request, { owner: nobody }), (e) => {
            assert.ok(e instanceof NatsNoRespondersError, `NatsNoRespondersError expected, got ${e}`);
            assert.equal(e.subject, `${nobody}.$rpc.v1.calc.mixed`);
            return true;
        });
        passed("no-responders");

        await assert.rejects(async () => jvm.call(CalcService.method.slow, {}, { timeoutMs: 300 }), (e) => {
            assert.ok(e instanceof NatsCallTimeoutError, `NatsCallTimeoutError expected, got ${e}`);
            assert.equal(e.subject, `${owner}.$rpc.v1.calc.slow`);
            assert.equal(e.timeoutMs, 300);
            return true;
        });
        passed("timeout");
    },

    async "call-answer"(nats, { owner }) {
        const jvm = nats.target(owner);
        // Only the secondary instance knows the key; the primary answers unknown.
        const known = await jvm.callAnswer(FinderService.method.find, { key: "secondary" }, { timeoutMs: 5000 });
        assert.equal(known.known, true, "known answer");
        assertMessage(FindReplySchema, known.value, { value: "from-secondary" }, "known answer");
        passed("known");
        const unknown = await jvm.callAnswer(FinderService.method.find, { key: "nobody" }, { timeoutMs: 1000 });
        assert.equal(unknown.known, false, "unknown answer");
        passed("unknown");
    },

    async "fan-out"(nats, { owner }) {
        assert.equal(await nats.target(owner).call(FinderService.method.broadcast, { message: "hello" }), undefined);
        passed("sent");
    },

    async "persistent-fire"(nats, { owner }) {
        assert.equal(await nats.target(owner).call(OrderService.method.place, { id: 42n }), undefined);
        passed("fired");
    },

    async "persistent-rpc"(nats, { owner }) {
        const jvm = nats.target(owner);
        assertMessage(NumberSchema, await jvm.call(OrderService.method.total, { id: 21n }), { value: 42n }, "total");
        passed("value");
        const lookup = await jvm.call(OrderService.method.lookup, { id: 1n });
        assertMessage(LookupReplySchema, lookup, {}, "lookup");
        assert.equal(isFieldSet(lookup, LookupReplySchema.field.value), false);
        passed("nullable");
    },

    async "large-core"(nats, { owner }) {
        const jvm = nats.target(owner);
        const checksum = await jvm.call(BlobService.method.checksum, { data: blob(LARGE) }, LARGE_CALL);
        assertMessage(NumberSchema, checksum, { value: BigInt(LARGE) }, "JS request offload resolved by the JVM");
        passed("request");
        assertBlob((await jvm.call(BlobService.method.create, { size: LARGE }, LARGE_CALL)).data, "JVM response");
        passed("response");
        assertBlob((await jvm.call(BlobService.method.echo, { data: blob(LARGE) }, LARGE_CALL)).data, "echo");
        passed("both");
    },

    async "large-persistent"(nats, { owner }) {
        const jvm = nats.target(owner);
        assertBlob((await jvm.call(OrderService.method.echo, { data: blob(LARGE) }, LARGE_CALL)).data, "persistent rpc");
        passed("rpc");
        assert.equal(await jvm.call(OrderService.method.big, { data: blob(LARGE) }, LARGE_CALL), undefined);
        passed("fire");
    },

    async versioned(nats, { owner }) {
        const jvm = nats.target(owner);
        const one = await jvm.call(V1VersionedService.method.ping, { message: "one" });
        assertMessage(V1PingReplySchema, one, { message: "one", version: 1 }, "v1");
        passed("v1");
        // attempt is left unset: Kotlin reads its [default = 1]
        const two = await jvm.call(V2VersionedService.method.ping, { message: "two" });
        assertMessage(V2PingReplySchema, two, { message: "two#1", version: 2, region: "eu" }, "v2");
        passed("v2");
    },

    async "global-service"(nats) {
        assertMessage(NumberSchema, await nats.call(RolesService.method.echo, { id: 5n }), { value: 5n }, "echo");
        passed("rpc");
        assert.equal(await nats.call(RolesService.method.whitelist, { action: "ADD", userId: 42n }), undefined);
        passed("fire");
    },

    vectors: runVectors,
};
