package dev.slne.surf.nats.gradle.contract

import com.squareup.wire.schema.Rpc
import dev.slne.surf.nats.protocol.ResolvedMethod

/**
 * An rpc the generator emits. Streaming rpcs and rpcs whose request or response cannot be resolved have none.
 */
internal class ContractMethod(
    val rpc: Rpc,
    val kotlinName: String,
    val request: RpcMessage,
    val response: RpcMessage,
    val resolution: ResolvedMethod,
)
