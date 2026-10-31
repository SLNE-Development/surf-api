package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.external.protobuf.DescFile

/**
 * A proto file that declares services, with the clients to generate for it.
 */
internal class ClientFile(val desc: DescFile, val services: List<ServiceClient>)
