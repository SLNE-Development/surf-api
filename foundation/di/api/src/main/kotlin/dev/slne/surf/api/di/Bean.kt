package dev.slne.surf.api.di

/**
 * Publishes the value of a property or the return value of a function as a bean.
 *
 * This is how a type the container cannot construct itself becomes injectable — a type from another
 * library, a type with a private constructor, anything that only a factory can produce:
 *
 * ```kotlin
 * @Service(scope = Scope.SHARED)
 * class RedisConnection : SurfComponent {
 *     @Bean(scope = Scope.SHARED)
 *     val api: RedisApi = RedisApi.create()
 *
 *     override suspend fun enable() = api.freezeAndConnect()
 *     override suspend fun disable() = api.disconnect()
 * }
 * ```
 *
 * Consumers then inject `RedisApi` and never learn that `RedisConnection` exists.
 *
 * The declaring class must itself be a bean. A factory function may take parameters, which the
 * container resolves like any other injection point. The published bean is resolved under the
 * declared type of the property or the return type of the function, so declare that type
 * explicitly rather than letting it be inferred — an inferred implementation type would be
 * published instead of the contract.
 *
 * The published bean's [scope] is its own and defaults to [Scope.SINGLETON] — it is not inherited
 * from the declaring bean, so a factory on a [Scope.SHARED] bean that should also be shared has to
 * say `@Bean(scope = Scope.SHARED)`. Inheriting would read better but cannot be expressed: an
 * omitted scope and an explicit `SINGLETON` are the same value, so the two cases are
 * indistinguishable.
 *
 * A factory carries no conditions of its own. Guard the declaring bean instead: if it is not
 * created, neither is anything it publishes.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Bean(val scope: Scope = Scope.SINGLETON)
