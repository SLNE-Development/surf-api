// Entry point of @slne/surf-nats. The runtime is the Kotlin/JS module; index.d.mts adds the protobuf-es generics.
export {
  connect,
  uuidToString,
  uuidFromString,
  NatsRpcError,
  NatsRemoteError,
  NatsNoRespondersError,
  NatsCallTimeoutError,
} from "./@slne/surf-nats.mjs";
