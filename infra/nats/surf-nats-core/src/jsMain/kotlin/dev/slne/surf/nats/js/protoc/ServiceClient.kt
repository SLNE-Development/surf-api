package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.external.protobuf.DescService

/**
 * A validated service for which a typed client is generated.
 *
 * [comment] is the JSDoc text of the `.proto` comment of the service, or `null` without one. [methods] lists the rpcs
 * in declaration order.
 */
internal class ServiceClient(val desc: DescService, val methods: List<ClientMethod>, val comment: String?) {
    /**
     * Name of the generated TypeScript interface: the service name plus `Client`.
     */
    val interfaceName: String get() = "${desc.name}Client"

    /**
     * Name of the generated factory function: `create` plus [interfaceName].
     */
    val factoryName: String get() = "create${desc.name}Client"
}
