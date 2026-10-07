package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
interface RpcCaller {
    suspend fun call(index: Int, args: Array<Any?>): Any?
}