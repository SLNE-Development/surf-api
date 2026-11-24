package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An [RpcCaller] that creates its [RpcCallerImpl] on the first call, so creating the proxy needs no
 * connection.
 *
 * The first call starts the delegate on [Dispatchers.IO], because starting a service with presence
 * creates a dispatcher on the connection. Later calls go straight to the delegate. Thread-safe.
 */
@InternalNatsApi
internal class LazyRpcCaller(
    private val client: NatsClientImpl,
    private val service: ServiceDescriptor,
) : RpcCaller {

    private val startLock = Any()

    @Volatile
    private var delegate: RpcCallerImpl? = null

    override suspend fun call(index: Int, request: Any): Any? {
        client.checkOpen()
        val caller = delegate ?: withContext(Dispatchers.IO) { start() }

        return caller.call(index, request)
    }

    private fun start(): RpcCallerImpl {
        return synchronized(startLock) {
            delegate ?: RpcCallerImpl(client, service).also {
                it.start()
                delegate = it
            }
        }
    }
}
