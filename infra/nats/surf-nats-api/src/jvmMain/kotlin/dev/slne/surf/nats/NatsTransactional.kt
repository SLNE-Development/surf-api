package dev.slne.surf.nats

import org.springframework.core.annotation.MergedAnnotations
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy

/**
 * Marks a [NatsService] bean whose persistent methods run at most once per message, each in one
 * database transaction.
 *
 * The handler's statements, the deduplication claim and the stored reply commit together; a
 * redelivered message receives the stored reply without running the handler again. The semantics
 * come from surf-microservice's reliability module; a context fails to start without it.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class NatsTransactional

/**
 * Whether this class, a superclass or an interface carries [NatsTransactional], also as a
 * meta-annotation.
 *
 * A [NatsServiceInterceptorProvider] that answers for every bean returns `null` for a class where
 * this is `true`. It needs `spring-core` on the class path.
 */
@InternalNatsApi
public fun Class<*>.isNatsTransactional(): Boolean {
    return MergedAnnotations.from(this, SearchStrategy.TYPE_HIERARCHY)
        .isPresent(NatsTransactional::class.java)
}
