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
 * - be either a regular or a `suspend` function
 *
 * ## Execution model
 * Event handler methods are invoked inside a coroutine launched on the internal Redis listener
 * scope, which uses **[kotlinx.coroutines.Dispatchers.Default]**.
 *
 * Regular (non-suspend) handlers run on a `Dispatchers.Default` thread.
 * `suspend` handlers are fully supported and benefit from structured concurrency automatically.
 *
 * Even though the handler runs in a coroutine, **do not perform blocking work directly**
 * (e.g. blocking I/O, blocking database drivers, `Thread.sleep`). Always switch the
 * dispatcher for blocking operations:
 *
 * ```
 * @OnRedisEvent
 * suspend fun onPlayerJoin(event: PlayerJoinEvent) {
 *     withContext(Dispatchers.IO) {
 *         // blocking work here
 *     }
 * }
 * ```
 *
 * ## Example
 * ```
 * // Regular handler
 * @OnRedisEvent
 * fun onPlayerJoin(event: PlayerJoinedEvent) {
 *     if (event.originatesFromThisClient()) return
 *     println("Player joined on another node: ${event.playerId}")
 * }
 *
 * // Suspend handler — can use suspend functions directly
 * @OnRedisEvent
 * suspend fun onPlayerJoinSuspend(event: PlayerJoinedEvent) {
 *     if (event.originatesFromThisClient()) return
 *     val profile = fetchProfileSuspending(event.playerId)
 *     println("Profile: $profile")
 * }
 *
 * // Blocking I/O — switch to Dispatchers.IO
 * @OnRedisEvent
 * suspend fun onPlayerJoinBlocking(event: PlayerJoinedEvent) {
 *     withContext(Dispatchers.IO) {
 *         loadFromDatabase(event.playerId)
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnRedisEvent
