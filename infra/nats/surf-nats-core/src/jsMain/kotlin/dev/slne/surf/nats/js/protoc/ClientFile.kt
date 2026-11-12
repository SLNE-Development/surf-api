package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.external.protobuf.DescFile

/**
 * A proto file that declares at least one service, with the clients to generate for it.
 *
 * [desc] is the file as protobuf-es describes it. [services] lists the services in declaration order.
 */
internal class ClientFile(val desc: DescFile, val services: List<ServiceClient>)
