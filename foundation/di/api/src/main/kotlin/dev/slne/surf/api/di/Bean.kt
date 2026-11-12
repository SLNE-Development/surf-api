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
 *     @Bean
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
 * [scope] defaults to [Scope.INHERIT], so a factory on a [Scope.SHARED] bean publishes a shared
 * bean without repeating it — which is what makes one connection visible to every application in
 * the process. State a scope to override that.
 *
 * A factory carries no conditions of its own. Guard the declaring bean instead: if it is not
 * created, neither is anything it publishes.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Bean(val scope: Scope = Scope.INHERIT)
