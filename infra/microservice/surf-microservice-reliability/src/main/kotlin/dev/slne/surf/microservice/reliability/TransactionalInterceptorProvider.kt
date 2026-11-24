package dev.slne.surf.microservice.reliability

import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.microservice.reliability.operation.TransactionalInterceptor
import dev.slne.surf.nats.*
import dev.slne.surf.nats.rpc.NatsPersistentInterceptor

/**
 * Gives every [NatsTransactional] service bean the interceptor that runs its persistent methods at
 * most once per message, in one READ COMMITTED transaction with its stored reply.
 *
 * It declares [handlesTransactional], which makes it the only provider that may answer for such a
 * bean.
 *
 * [owner] namespaces the stored replies. Instances of one service share it, so they answer each
 * other's redeliveries; changing it lets messages stored under the old owner run their handlers
 * again.
 */
@OptIn(InternalNatsApi::class)
internal class TransactionalInterceptorProvider(
    private val owner: String,
    private val operations: AtomicOperations,
) : NatsServiceInterceptorProvider {

    override val handlesTransactional: Boolean get() = true

    override fun interceptorFor(
        bean: Any,
        beanClass: Class<*>,
        client: NatsClient,
    ): NatsPersistentInterceptor? {
        return if (beanClass.isNatsTransactional()) {
            TransactionalInterceptor(owner, operations, client)
        } else null
    }
}
