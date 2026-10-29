// Entry point of @slne/surf-nats. The runtime is the Kotlin/JS module; index.d.mts adds the protobuf-es generics.
import {connect as connectKotlin} from "./@slne/surf-nats.mjs";

export {
  NatsRpcError,
  NatsRemoteError,
  NatsNoRespondersError,
  NatsCallTimeoutError,
} from "./@slne/surf-nats.mjs";

// ConnectOptions.servers is string | string[] here and string[] in Kotlin; a string lists servers separated by commas.
export function connect(options) {
  const servers = options?.servers;
  return connectKotlin(
      typeof servers === "string" ?
          {...options, servers: servers.split(",")}
          : options
  );
}

// surf.types.v1.Uuid holds 16 bytes, the most significant half first (big-endian), as the JVM UuidSerializer writes.
const UUID_TYPE_NAME = "surf.types.v1.Uuid";
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function uuidToString(uuid) {
  const bytes = uuid instanceof Uint8Array ? uuid : uuid?.value;
  if (!(bytes instanceof Uint8Array) || bytes.length !== 16) {
    throw new RangeError(
        `${UUID_TYPE_NAME} value must be exactly 16 bytes, was ${bytes?.length
        ?? bytes}`
    );
  }
  const hex = Array.from(bytes,
      (byte) => byte.toString(16).padStart(2, "0")
  ).join("");

  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12,
      16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function uuidFromString(value) {
  if (typeof value !== "string" || !UUID_PATTERN.test(value)) {
    throw new RangeError(
        `Invalid UUID '${value}': expected 8-4-4-4-12 hexadecimal digits`);
  }
  const hex = value.replaceAll("-", "");
  const bytes = new Uint8Array(16);
  for (let index = 0; index < 16; index++) {
    bytes[index] = Number.parseInt(hex.slice(index * 2, index * 2 + 2), 16);
  }
  return {$typeName: UUID_TYPE_NAME, value: bytes};
}
