// Entry point of @slne/surf-api-nats. The runtime is the Kotlin/JS module; index.d.mts adds the protobuf-es generics.
export {
  connect,
  uuidToString,
  uuidFromString,
  toPlain,
  NatsRpcError,
  NatsRemoteError,
  NatsNoRespondersError,
  NatsCallTimeoutError,
} from "./@slne/surf-api-nats.mjs";
