#!/usr/bin/env node
// protoc-gen-surf-nats: generates typed surf-nats clients next to the protoc-gen-es output (see runProtocGenSurfNats).
import { runProtocGenSurfNats } from "../@slne/surf-nats.mjs";

await runProtocGenSurfNats();
