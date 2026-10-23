// JVM <-> JS interop harness: `node interop.mjs <scenario>`, started by JsInteropTest (jvmInteropTest) in the staging
// directory that prepareJsInterop fills with the npm distribution of @slne/surf-nats and the generated fixtures.
// SURF_NATS_SERVERS names the NATS server; a failed check exits with a non-zero code.
import { connect } from "@slne/surf-nats";
import { scenarios } from "./scenarios.mjs";
import { context } from "./support.mjs";

const name = process.argv[2];
const scenario = scenarios[name];
if (!scenario) throw new Error(`Unknown scenario '${name}', expected one of ${Object.keys(scenarios).join(", ")}`);

const settings = context();
const nats = await connect({ name: "interop-js", servers: process.env.SURF_NATS_SERVERS });
try {
    await scenario(nats, settings);
} finally {
    await nats.close();
}
