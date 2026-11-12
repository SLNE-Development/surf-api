package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi

/**
 * Sends the calls of a generated service proxy.
 */
@InternalNatsApi
internal interface RpcCaller {
    /**
     * Calls the method at [index] of the service descriptor with [request] and returns its decoded result.
     *
     * [request] is [Unit] for a method with a `google.protobuf.Empty` request.
     */
    suspend fun call(index: Int, request: Any): Any?
}
