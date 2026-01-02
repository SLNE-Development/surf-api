package dev.slne.surf.redis.event

/**
 * Marks a method as a Redis event handler.
 *
 * Methods annotated with this annotation must:
 * - Have **exactly one parameter**
 * - The parameter type must be a subtype of [RedisEvent]
 * - **Not** be a `suspend` function
 *
 * Event handler methods are invoked synchronously on a Redisson/Reactor
 * thread. If asynchronous or suspending work is required,
 * the handler must explicitly launch its own coroutine.
 *
 * Example:
 * ```
 * @OnRedisEvent
 * fun onPlayerJoin(event: PlayerJoinEvent) {
 *     // Handle event synchronously
 * }
 *
 * @OnRedisEvent
 * fun onPlayerJoinAsync(event: PlayerJoinEvent) {
 *     coroutineScope.launch {
 *         // Handle event asynchronously
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnRedisEvent
