// Flow scenarios of the interop script; each one matches a test of JsInteropTest and reports the checks it expects.
// Calls go through the clients of protoc-gen-surf-nats (*_nats.js); subjects in the assertions are literal on purpose.
import assert from "node:assert/strict";
import { isFieldSet } from "@bufbuild/protobuf";
import {
    NatsCallTimeoutError,
    NatsNoRespondersError,
    NatsRemoteError,
    NatsRpcError,
} from "@slne/surf-nats";
import { createUnservedServiceClient } from "./gen/surf/nats/test/interop/v1/unserved_nats.js";
import { createBlobServiceClient } from "./gen/surf/nats/test/v1/blobs_nats.js";
import { createCalcServiceClient } from "./gen/surf/nats/test/v1/calc_nats.js";
import { Color } from "./gen/surf/nats/test/v1/colors_pb.js";
import { NumberSchema, TextSchema } from "./gen/surf/nats/test/v1/common_pb.js";
import { createFinderServiceClient } from "./gen/surf/nats/test/v1/finder_nats.js";
import { FindReplySchema } from "./gen/surf/nats/test/v1/finder_pb.js";
import { InteropEventSchema } from "./gen/surf/nats/test/v1/interop_pb.js";
import { createOrderServiceClient } from "./gen/surf/nats/test/v1/orders_nats.js";
import { LookupReplySchema } from "./gen/surf/nats/test/v1/orders_pb.js";
import { createRolesServiceClient } from "./gen/surf/nats/test/v1/roles_nats.js";
import { createVersionedServiceClient as createV1VersionedClient } from "./gen/surf/nats/test/v1/versioned_nats.js";
import { VersionedPingReplySchema as V1PingReplySchema } from "./gen/surf/nats/test/v1/versioned_pb.js";
import { createVersionedServiceClient as createV2VersionedClient } from "./gen/surf/nats/test/v2/versioned_nats.js";
import { VersionedPingReplySchema as V2PingReplySchema } from "./gen/surf/nats/test/v2/versioned_pb.js";
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

// Mirrors JsInteropTest.LOOKUPS
const LOOKUPS = 20;

function assertJvmOrigin(context, label) {
    assert.ok(context.origin?.startsWith("interop-jvm@"), `${label}: Surf-Origin was ${context.origin}`);
}

function assertBlob(actual, label) {
    assert.ok(Buffer.from(actual).equals(Buffer.from(blob(LARGE))), `${label}: ${actual.length} bytes, content differs`);
}

export const scenarios = {
    async "publish-events"(nats) {
        await nats.publish(InteropEventSchema, EVENT);
        passed("published");
    },

    async "listen-events"(nats) {
        const all = [];
        const any = [];
        await nats.listen(InteropEventSchema, (event, context) => all.push({ event, context }));
        await nats.listen(InteropEventSchema, (event, context) => any.push({ event, context }), {
            recipients: "ANY_INSTANCE",
        });
        ready("listening");

        await until(() => all.length > 0 && any.length > 0, "the JVM event");
        await sleep(SETTLE_MS);
        assert.equal(all.length, 1, "ALL_INSTANCES deliveries");
        assertMessage(InteropEventSchema, all[0].event, EVENT, "ALL_INSTANCES listener");
        assertJvmOrigin(all[0].context, "ALL_INSTANCES listener");
        passed("all");
        assert.equal(any.length, 1, "ANY_INSTANCE deliveries");
        assertMessage(InteropEventSchema, any[0].event, EVENT, "ANY_INSTANCE listener");
        assertJvmOrigin(any[0].context, "ANY_INSTANCE listener");
        passed("any");
    },

    async "call-any"(nats) {
        const calc = createCalcServiceClient(nats);
        const text = await calc.mixed({ a: -3, b: 9007199254740993n, factor: 0.5, flag: true, label: "surf" });
        assertMessage(TextSchema, text, { value: "-3|9007199254740993|0.5|true|surf" }, "mixed");
        passed("value");
        assert.equal(await calc.fire({}), undefined);
        passed("unit");
    },

    async "call-errors"(nats) {
        const calc = createCalcServiceClient(nats);
        await assert.rejects(async () => calc.fail({ message: "boom" }), (e) => {
            assert.ok(e instanceof NatsRemoteError, `NatsRemoteError expected, got ${e}`);
            assert.ok(e instanceof NatsRpcError);
            assert.equal(e.type, "java.lang.IllegalStateException");
            assert.equal(e.remoteMessage, "boom");
            return true;
        });
        passed("remote-error");

        await assert.rejects(async () => createUnservedServiceClient(nats).ping({}), (e) => {
            assert.ok(e instanceof NatsNoRespondersError, `NatsNoRespondersError expected, got ${e}`);
            assert.equal(e.subject, "surf.rpc.surf.nats.test.interop.v1.UnservedService.Ping");
            return true;
        });
        passed("no-responders");

        await assert.rejects(async () => calc.slow({}, { timeoutMs: 300 }), (e) => {
            assert.ok(e instanceof NatsCallTimeoutError, `NatsCallTimeoutError expected, got ${e}`);
            assert.equal(e.subject, "surf.rpc.surf.nats.test.v1.CalcService.Slow");
            assert.equal(e.timeoutMs, 300);
            return true;
        });
        passed("timeout");
    },

    async "call-any-instances"(nats) {
        // Both JVM instances serve FinderService in the queue group of its full name
        const finder = createFinderServiceClient(nats);
        const counts = new Map();
        for (let i = 0; i < LOOKUPS; i++) {
            const { value } = await finder.lookup({ key: "any" });
            counts.set(value, (counts.get(value) ?? 0) + 1);
        }
        assert.deepEqual([...counts.keys()].sort(), ["from-primary", "from-secondary"], "answering instances");
        passed("both-instances");
    },

    async "call-answer"(nats) {
        const finder = createFinderServiceClient(nats);
        // Only the secondary instance knows the key; the primary answers unknown.
        const known = await finder.find({ key: "secondary" }, { timeoutMs: 5000 });
        assert.equal(known.known, true, "known answer");
        assertMessage(FindReplySchema, known.value, { value: "from-secondary" }, "known answer");
        passed("known");
        const unknown = await finder.find({ key: "nobody" }, { timeoutMs: 1000 });
        assert.equal(unknown.known, false, "unknown answer");
        passed("unknown");
    },

    async "fan-out"(nats) {
        assert.equal(await createFinderServiceClient(nats).broadcast({ message: "hello" }), undefined);
        passed("sent");
    },

    async "persistent-fire"(nats) {
        assert.equal(await createOrderServiceClient(nats).place({ id: 42n }), undefined);
        passed("fired");
    },

    async "persistent-fire-failing"(nats) {
        assert.equal(await createOrderServiceClient(nats).paint({ color: Color.RED }), undefined);
        passed("fired");
    },

    async "persistent-rpc"(nats) {
        const orders = createOrderServiceClient(nats);
        assertMessage(NumberSchema, await orders.total({ id: 21n }), { value: 42n }, "total");
        passed("value");
        const lookup = await orders.lookup({ id: 1n });
        assertMessage(LookupReplySchema, lookup, {}, "lookup");
        assert.equal(isFieldSet(lookup, LookupReplySchema.field.value), false);
        passed("nullable");
        await assert.rejects(async () => orders.audit({ id: 7n }), (e) => {
            assert.ok(e instanceof NatsRemoteError, `NatsRemoteError expected, got ${e}`);
            assert.equal(e.type, "java.lang.IllegalStateException");
            assert.equal(e.remoteMessage, "audit 7 rejected");
            return true;
        });
        passed("remote-error");
    },

    async "large-core"(nats) {
        const blobs = createBlobServiceClient(nats);
        const checksum = await blobs.checksum({ data: blob(LARGE) }, LARGE_CALL);
        assertMessage(NumberSchema, checksum, { value: BigInt(LARGE) }, "JS request offload resolved by the JVM");
        passed("request");
        assertBlob((await blobs.create({ size: LARGE }, LARGE_CALL)).data, "JVM response");
        passed("response");
        assertBlob((await blobs.echo({ data: blob(LARGE) }, LARGE_CALL)).data, "echo");
        passed("both");
    },

    async "large-persistent"(nats) {
        const orders = createOrderServiceClient(nats);
        assertBlob((await orders.echo({ data: blob(LARGE) }, LARGE_CALL)).data, "persistent rpc");
        passed("rpc");
        assert.equal(await orders.big({ data: blob(LARGE) }, LARGE_CALL), undefined);
        passed("fire");
    },

    async versioned(nats) {
        const one = await createV1VersionedClient(nats).ping({ message: "one" });
        assertMessage(V1PingReplySchema, one, { message: "one", version: 1 }, "v1");
        passed("v1");
        // attempt is left unset: Kotlin reads its [default = 1]
        const two = await createV2VersionedClient(nats).ping({ message: "two" });
        assertMessage(V2PingReplySchema, two, { message: "two#1", version: 2, region: "eu" }, "v2");
        passed("v2");
    },

    async "full-name-addressing"(nats) {
        const roles = createRolesServiceClient(nats);
        assertMessage(NumberSchema, await roles.echo({ id: 5n }), { value: 5n }, "echo");
        passed("rpc");
        assert.equal(await roles.whitelist({ action: "ADD", userId: 42n }), undefined);
        passed("fire");
    },

    vectors: runVectors,
};
