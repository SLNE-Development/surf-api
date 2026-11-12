package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.protocol.ResolvedMethod

/**
 * A validated rpc of a [ServiceClient] with its options resolved against the service.
 *
 * [comment] is the JSDoc text of the `.proto` comment of the rpc, or `null` without one.
 */
internal class ClientMethod(val desc: DescMethod, val resolved: ResolvedMethod, val comment: String?)
