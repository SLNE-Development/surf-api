package dev.slne.surf.microservice.reliability

import dev.slne.surf.database.reliability.ReliabilityMigrationSet
import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.database.spring.SurfDatabaseAutoConfiguration
import dev.slne.surf.microservice.api.MicroserviceInfo
import dev.slne.surf.microservice.reliability.operation.StoredReplyCleanup
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

/**
 * Provides the idempotent handlers of a microservice.
 *
 * - [ReliabilityConfig], unless the application defines one.
 * - [ReliabilityMigrationSet], which creates the stored-reply table after the service's own
 *   migrations.
 * - [AtomicOperations] on the microservice's database, configured by
 *   [ReliabilityConfig.atomicOperations], unless the application defines one. The interceptor
 *   provider and the cleanup use that single instance.
 * - [TransactionalEvents], the publisher that `@NatsTransactional` handlers inject, unless the
 *   application defines one.
 * - The interceptor provider that makes every `@NatsTransactional` service bean idempotent; the
 *   stored replies belong to the microservice's name.
 * - The periodic cleanup of expired stored replies, which runs while the context runs.
 *
 * An application [AtomicOperations] bean replaces the default for the interceptor and the cleanup
 * alike. It must be built on the microservice's own [R2dbcDatabase] and keep a `retention` of at
 * least `NatsPersistentLimits.REDELIVERY_HORIZON`. Another database loses the stored replies of the
 * service's transactions, and a shorter retention lets a late redelivery or dead-letter replay run
 * its handler again; either breaks the at-most-once guarantee.
 */
@AutoConfiguration(after = [SurfDatabaseAutoConfiguration::class])
@ConditionalOnBean(MicroserviceInfo::class)
public class MicroserviceReliabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public fun reliabilityConfig(): ReliabilityConfig = ReliabilityConfig()

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public fun reliabilityMigrationSet(): ReliabilityMigrationSet = ReliabilityMigrationSet()

    @Bean
    @ConditionalOnMissingBean
    public fun atomicOperations(
        database: R2dbcDatabase,
        config: ReliabilityConfig,
    ): AtomicOperations {
        return AtomicOperations(database, config.atomicOperations)
    }

    @Bean
    @ConditionalOnMissingBean
    public fun transactionalEvents(): TransactionalEvents = TransactionalEvents()

    @Bean(TRANSACTIONAL_INTERCEPTORS_BEAN)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    internal fun transactionalInterceptorProvider(
        info: MicroserviceInfo,
        operations: AtomicOperations,
    ): TransactionalInterceptorProvider {
        return TransactionalInterceptorProvider(info.name, operations)
    }

    @Bean(STORED_REPLY_CLEANUP_BEAN, destroyMethod = "close")
    internal fun storedReplyCleanup(
        operations: AtomicOperations,
        config: ReliabilityConfig,
    ): StoredReplyCleanup {
        return StoredReplyCleanup(operations, config.cleanupInterval)
    }

    private companion object {
        const val TRANSACTIONAL_INTERCEPTORS_BEAN = "transactionalInterceptorProvider"
        const val STORED_REPLY_CLEANUP_BEAN = "storedReplyCleanup"
    }
}
