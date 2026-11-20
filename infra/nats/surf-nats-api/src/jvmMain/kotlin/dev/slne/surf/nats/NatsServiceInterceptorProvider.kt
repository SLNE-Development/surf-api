package dev.slne.surf.nats

import dev.slne.surf.nats.rpc.NatsPersistentInterceptor

/**
 * Supplies the interceptor that wraps the persistent methods of a [NatsService] bean.
 *
 * When a context starts, it asks every provider bean, in `@Order` order, about each `@NatsService` bean and registers
 * the service with the first interceptor returned. A bean no provider answers for is registered without one.
 *
 * A [NatsTransactional] bean is stricter, because an interceptor cannot be chained: exactly one provider may answer
 * for it, and that provider must declare [handlesTransactional]. Otherwise the context fails to start. A provider that
 * answers for every bean, such as one for metrics or tracing, must therefore return `null` for such a bean; check it
 * with [isNatsTransactional].
 */
@InternalNatsApi
public fun interface NatsServiceInterceptorProvider {

    /**
     * Whether this provider gives [NatsTransactional] beans their at-most-once semantics. Defaults to `false`.
     */
    public val handlesTransactional: Boolean get() = false

    /**
     * Returns the interceptor for [bean], whose class behind any proxy is [beanClass], on [client], or `null` if this
     * provider does not handle the bean.
     */
    public fun interceptorFor(bean: Any, beanClass: Class<*>, client: NatsClient): NatsPersistentInterceptor?
}
