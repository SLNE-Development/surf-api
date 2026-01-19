package dev.slne.surf.redis.event

/**
 * Marks a method as a Redis event handler.
 *
 * Methods annotated with this annotation are discovered and registered
 * by the Redis event bus when a listener object is registered.
 *
 * ## Requirements
 * An event handler method must:
 * - have **exactly one parameter**
 * - accept a parameter whose type is a subtype of [RedisEvent]
 * - **not** be a `suspend` function
 *
 * ## Execution model
 * Event handler methods are invoked synchronously on a Redisson/Reactor thread.
 * If asynchronous or suspending work is required, the handler must explicitly
 * delegate that work (for example by launching a coroutine).
 *
 * ## Example
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
