// Wire vectors of spec 12.7; InteropVectors (jvmTest) holds the Kotlin values under the same names.
//
// Per vector: the JVM encodes its Kotlin value (InteropVectorService), surf-nats-js decodes and normalises it and the
// script compares it with `expect` and its own re-encoding with the Kotlin bytes. Then the script sends the decoded
// message and its own message (`send`, default `expect`) back through InteropService: the JVM compares what Kotlin
// decodes with its expectation and replies with it, and the script compares the reply with `expectEcho` (default
// `expect`). `raw` vectors come from RawVectorResponder; `reject` vectors must fail on the JS side. `unknownFields`
// lists the field numbers the decoded message keeps as unknown fields (a newer sender's fields).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { create, isFieldSet, toBinary } from "@bufbuild/protobuf";
import { Color, ColorHolderSchema } from "./gen/surf/nats/test/v1/colors_pb.js";
import {
    InteropDefaultsSchema,
    InteropEmptySchema,
    InteropEvolvedSchema,
    InteropEvolvedV2Schema,
    InteropMapsSchema,
    InteropNullablesSchema,
    InteropOneofSchema,
    InteropPackedSchema,
    InteropRequiredSchema,
    InteropScalarsSchema,
    InteropService,
    InteropTypesSchema,
    InteropVectorService,
} from "./gen/surf/nats/test/v1/interop_pb.js";
import { assertMessage, hex, UUID_BYTES, vectorPassed } from "./support.mjs";

const KINDS = {
    scalars: [InteropScalarsSchema, InteropVectorService.method.scalars, InteropService.method.echoScalars],
    maps: [InteropMapsSchema, InteropVectorService.method.maps, InteropService.method.echoMaps],
    packed: [InteropPackedSchema, InteropVectorService.method.packed, InteropService.method.echoPacked],
    colors: [ColorHolderSchema, InteropVectorService.method.colors, InteropService.method.echoColors],
    nullables: [InteropNullablesSchema, InteropVectorService.method.nullables, InteropService.method.echoNullables],
    defaults: [InteropDefaultsSchema, InteropVectorService.method.defaults, InteropService.method.echoDefaults],
    required: [InteropRequiredSchema, InteropVectorService.method.required, InteropService.method.echoRequired],
    types: [InteropTypesSchema, InteropVectorService.method.types, InteropService.method.echoTypes],
    empty: [InteropEmptySchema, InteropVectorService.method.empty, InteropService.method.echoEmpty],
    oneof: [InteropOneofSchema, InteropVectorService.method.oneof, InteropService.method.echoOneof],
    evolved: [InteropEvolvedSchema, InteropVectorService.method.evolved, InteropService.method.echoEvolved],
    evolvedV2: [InteropEvolvedV2Schema, InteropVectorService.method.evolvedV2, InteropService.method.echoEvolvedV2],
};

const ZERO_SCALARS = {
    unsignedSmall: 0,
    unsignedLarge: 0n,
    signedLarge: 0n,
    small: 0,
    large: 0n,
    fixedSmall: 0,
    ratio: 0,
    flag: false,
};

// What protobuf-es exposes for unset fields with a proto [default]
const DEFAULTS = {
    number: 7,
    text: "surf",
    flag: true,
    ratio: 1.5,
    color: Color.UNSPECIFIED,
    data: Uint8Array.of(1, 2),
    unsignedSmall: 4294967295,
};

// WIDE_COLOR_PURPLE (4) normalised to the zero entry, as Kotlin decodes it
const UNKNOWN_COLORS_NORMALISED = {
    color: Color.UNSPECIFIED,
    maybeColor: Color.UNSPECIFIED,
    colors: [Color.UNSPECIFIED, Color.RED],
    colorsByName: { a: Color.UNSPECIFIED, b: Color.GREEN },
};

function assertUnset(schema, message, label, except = []) {
    for (const field of schema.fields) {
        if (except.includes(field.localName)) continue;
        assert.equal(isFieldSet(message, field), false, `${label}: ${field.localName} must be unset`);
    }
}

const VECTORS = [
    {
        name: "scalars-extremes",
        kind: "scalars",
        expect: {
            unsignedSmall: 4294967295,
            unsignedLarge: 18446744073709551615n,
            signedLarge: -9223372036854775808n,
            small: -2147483648,
            large: -9223372036854775808n,
            fixedSmall: 4294967295,
            ratio: -0.25,
            flag: true,
        },
        // Kotlin writes uint32 values >= 2^31 as 10-byte varints (int32 encoding), protobuf-es as 5 bytes
        sameBytes: false,
    },
    {
        name: "scalars-2^31",
        kind: "scalars",
        expect: {
            unsignedSmall: 2147483648,
            unsignedLarge: 9223372036854775808n,
            signedLarge: 9223372036854775807n,
            small: 2147483647,
            large: 9223372036854775807n,
            fixedSmall: 2147483648,
            ratio: 1e300,
            flag: false,
        },
        sameBytes: false,
    },
    // Non-null fields without [default] are on the wire even when zero
    { name: "scalars-zero", kind: "scalars", expect: ZERO_SCALARS },
    {
        name: "maps",
        kind: "maps",
        expect: {
            signedByName: {
                min: -9223372036854775808n,
                "minus-one": -1n,
                zero: 0n,
                max: 9223372036854775807n,
            },
            namesByFixed: { 0: "zero", 1: "one", 2147483648: "2^31", 4294967295: "2^32-1" },
        },
    },
    { name: "maps-empty", kind: "maps", expect: {} },
    {
        name: "packed",
        kind: "packed",
        expect: { packedValues: [0, 1, -1, 2147483647, -2147483648, 300], unpackedValues: [7, -7, 0] },
    },
    { name: "packed-empty", kind: "packed", expect: {} },
    {
        name: "colors-known",
        kind: "colors",
        expect: {
            color: Color.RED,
            maybeColor: Color.BLUE,
            colors: [Color.GREEN, Color.UNSPECIFIED, Color.BLUE],
            colorsByName: { a: Color.RED, zero: Color.UNSPECIFIED },
        },
    },
    {
        name: "colors-unknown-from-kotlin",
        kind: "colors",
        raw: true,
        expect: UNKNOWN_COLORS_NORMALISED,
        // The re-encoding carries the normalised numbers
        sameBytes: false,
    },
    {
        name: "colors-unknown-from-js",
        kind: "colors",
        expect: UNKNOWN_COLORS_NORMALISED,
        send: { color: 4, maybeColor: 4, colors: [4, Color.RED], colorsByName: { a: 4, b: Color.GREEN } },
    },
    {
        name: "nullables-null",
        kind: "nullables",
        expect: {},
        check: (message) => assertUnset(InteropNullablesSchema, message, "nullables-null"),
    },
    {
        name: "nullables-zero",
        kind: "nullables",
        expect: {
            text: "",
            number: 0,
            data: new Uint8Array(0),
            scalars: ZERO_SCALARS,
            uuid: { value: new Uint8Array(16) },
            color: Color.UNSPECIFIED,
        },
    },
    {
        // Kotlin elides every field except the bytes one (kotlinx compares the ByteArray default by reference)
        name: "defaults-elided",
        kind: "defaults",
        expect: { data: DEFAULTS.data },
        check: (message) => {
            assertUnset(InteropDefaultsSchema, message, "defaults-elided", ["data"]);
            for (const [field, value] of Object.entries(DEFAULTS)) {
                const actual = message[field];
                // Decoded bytes are Buffer views; compare their content
                assert.deepEqual(
                    value instanceof Uint8Array ? hex(actual) : actual,
                    value instanceof Uint8Array ? hex(value) : value,
                    `defaults-elided: ${field} reads its [default]`,
                );
            }
            assert.equal(hex(toBinary(InteropDefaultsSchema, create(InteropDefaultsSchema, {}))), "");
        },
    },
    {
        name: "defaults-explicit",
        kind: "defaults",
        expect: {
            number: 0,
            text: "",
            flag: false,
            ratio: 0,
            color: Color.RED,
            data: new Uint8Array(0),
            unsignedSmall: 0,
        },
    },
    {
        // JS sends the default values explicitly, Kotlin reads them and elides them again in its reply
        name: "defaults-sent-by-js",
        kind: "defaults",
        expect: { data: DEFAULTS.data },
        send: DEFAULTS,
        check: () => assert.notEqual(hex(toBinary(InteropDefaultsSchema, create(InteropDefaultsSchema, DEFAULTS))), ""),
    },
    {
        name: "required",
        kind: "required",
        expect: { name: "steve" },
        check: async (_, { nats, owner }) => {
            // A non-null field omitted by the TS sender fails the local validation, nothing is sent
            await assert.rejects(
                async () => nats.call(InteropService.method.echoRequired, { count: 1 }, { owner }),
                /required field 'name' is not set/,
            );
        },
    },
    { name: "required-empty", kind: "required", expect: { name: "", count: 0 } },
    {
        // Kotlin bytes without InteropRequired.name fail the JS validation of the reply
        name: "required-missing",
        kind: "required",
        raw: true,
        reject: /required field 'name' is not set/,
    },
    {
        name: "types",
        kind: "types",
        expect: {
            uuid: { value: UUID_BYTES },
            timestamp: { seconds: 1700000000n, nanos: 123456789 },
            duration: { seconds: -1n, nanos: -500000000 },
        },
    },
    {
        // Epoch Timestamp and zero Duration are present but empty messages
        name: "types-epoch",
        kind: "types",
        expect: {
            uuid: { value: Uint8Array.from(Buffer.from("80000000000000000000000000000001", "hex")) },
            timestamp: {},
            duration: {},
        },
    },
    {
        name: "types-negative-subsecond",
        kind: "types",
        expect: {
            uuid: { value: UUID_BYTES },
            timestamp: { seconds: -1n, nanos: 999999999 },
            duration: { nanos: -1 },
        },
    },
    { name: "empty", kind: "empty", expect: {} },
    {
        name: "oneof-unset",
        kind: "oneof",
        expect: {},
        check: (message) => assert.equal(message.value.case, undefined),
    },
    { name: "oneof-text", kind: "oneof", expect: { value: { case: "text", value: "" } } },
    { name: "oneof-number", kind: "oneof", expect: { value: { case: "number", value: -9223372036854775808n } } },
    { name: "oneof-empty", kind: "oneof", expect: { value: { case: "empty", value: {} } } },
    {
        // A newer Kotlin sender: the older schema keeps nickname (3) and rank (4) as unknown fields, normalisation and
        // validation ignore them, and the echo sends them on to the older Kotlin reader, which skips them
        name: "evolved-new-to-old",
        kind: "evolved",
        raw: true,
        expect: { name: "steve", level: 3 },
        unknownFields: [3, 4],
    },
    {
        // An older Kotlin sender: the newer schema reads nickname as unset and rank as its [default]
        name: "evolved-old-to-new",
        kind: "evolvedV2",
        raw: true,
        expect: { name: "alex", level: 0 },
        check: (message) => {
            assertUnset(InteropEvolvedV2Schema, message, "evolved-old-to-new", ["name", "level"]);
            assert.equal(message.rank, 5, "evolved-old-to-new: rank reads its [default]");
        },
    },
];

async function runVector(nats, context, kotlinHex, vector) {
    const [schema, fetch, echo] = KINDS[vector.kind];
    const label = `vector ${vector.name}`;
    const owner = context.owner;
    const fetchOwner = vector.raw ? context.rawOwner : owner;
    const fetched = async () => nats.call(fetch, { name: vector.name }, { owner: fetchOwner });

    if (vector.reject) {
        await assert.rejects(fetched, vector.reject, `${label}: the reply must be rejected`);
        return;
    }

    const decoded = await fetched();
    if (vector.unknownFields) {
        assertMessage(schema, decoded, vector.expect, `${label}: Kotlin -> JS`, { unknown: false });
        assert.deepEqual((decoded.$unknown ?? []).map((field) => field.no), vector.unknownFields, `${label}: unknown fields`);
    } else {
        assertMessage(schema, decoded, vector.expect, `${label}: Kotlin -> JS`);
    }
    if (vector.sameBytes !== false) {
        assert.equal(hex(toBinary(schema, decoded)), kotlinHex[vector.name], `${label}: JS re-encoding of the Kotlin bytes`);
    }
    await vector.check?.(decoded, { nats, owner });

    const expectEcho = vector.expectEcho ?? vector.expect;
    assertMessage(schema, await nats.call(echo, decoded, { owner }), expectEcho, `${label}: echo of the decoded message`);
    const sent = vector.send ?? vector.expect;
    assertMessage(schema, await nats.call(echo, sent, { owner }), expectEcho, `${label}: echo of the JS message`);
}

export async function runVectors(nats, context) {
    const kotlinHex = JSON.parse(readFileSync(context.vectorsFile, "utf8"));
    for (const vector of VECTORS) {
        await runVector(nats, context, kotlinHex, vector);
        vectorPassed(vector.name);
    }
}
