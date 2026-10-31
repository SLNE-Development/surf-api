package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.protocol.ResolvedMethod

/**
 * A validated rpc of a [ServiceClient] with its resolved options and the JSDoc text of its `.proto` comment.
 */
internal class ClientMethod(val desc: DescMethod, val resolved: ResolvedMethod, val comment: String?)
