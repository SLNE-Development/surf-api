package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.external.protobuf.DescService

/**
 * A validated service for which a typed client is generated.
 */
internal class ServiceClient(val desc: DescService, val methods: List<ClientMethod>, val comment: String?) {
    val interfaceName: String get() = "${desc.name}Client"
    val factoryName: String get() = "create${desc.name}Client"
}
