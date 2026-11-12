// Helpers of the interop script: markers read by NodeScript (JsInteropTest), message assertions and test data.
import assert from "node:assert/strict";
import { create, equals } from "@bufbuild/protobuf";

export const LARGE = 2 * 1024 * 1024;
export const SETTLE_MS = 500;

// 123e4567-e89b-12d3-a456-426614174000 (TEST_UUID of the JVM tests), most significant byte first
export const UUID_BYTES = Uint8Array.from(Buffer.from("123e4567e89b12d3a456426614174000", "hex"));

/**
 * Files passed by JsInteropTest.
 */
export function context() {
    return {
        vectorsFile: required("SURF_NATS_INTEROP_VECTORS"),
    };
}

function required(name) {
    const value = process.env[name];
    if (!value) throw new Error(`${name} is not set`);
    return value;
}

export function ready(step) {
    console.log(`@ready ${step}`);
}

export function passed(check) {
    console.log(`@check ${check}`);
}

export function vectorPassed(name) {
    console.log(`@vector ${name}`);
}

/**
 * Asserts that `actual` equals `create(schema, expectedInit)`, field presence and (unless `unknown` is false) unknown
 * fields included.
 */
export function assertMessage(schema, actual, expectedInit, label, { unknown = true } = {}) {
    const expected = create(schema, expectedInit);
    assert.ok(
        equals(schema, actual, expected, { unknown }),
        `${label}\n  expected ${show(expected)}\n  actual   ${show(actual)}`,
    );
}

export function show(value) {
    return JSON.stringify(value, (_, v) => {
        if (typeof v === "bigint") return `${v}n`;
        if (v instanceof Uint8Array) return `0x${hex(v)}`;
        return v;
    });
}

export function hex(bytes) {
    return Buffer.from(bytes).toString("hex");
}

// Mirrors JsInteropTest.blob
export function blob(size) {
    const data = new Uint8Array(size);
    for (let i = 0; i < size; i++) data[i] = (i * 31 + Math.floor(i / 7)) & 0xff;
    return data;
}

export function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function until(condition, label, timeoutMs = 10_000) {
    const deadline = Date.now() + timeoutMs;
    while (!condition()) {
        if (Date.now() > deadline) throw new Error(`Timed out after ${timeoutMs} ms waiting for ${label}`);
        await sleep(20);
    }
}
