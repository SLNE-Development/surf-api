package dev.slne.surf.redis.request

/**
 * Marks a method as a Redis request handler.
 *
 * Methods annotated with this annotation are discovered and registered by the
 * Redis request/response bus to handle incoming [RedisRequest]s.
 *
 * ## Requirements
 * A request handler method must:
 * - have **exactly one parameter** of type [RequestContext]<T>
 * - where `T` is a subtype of [RedisRequest]
 * - **not** be a `suspend` function
 * - return `Unit`
 *
 * ## Execution model
 * Request handler methods are invoked synchronously on a Redisson/Reactor thread.
 * If asynchronous or suspending work is required, the handler must explicitly
 * delegate that work (for example by launching a coroutine) and call
 * [RequestContext.respond] from there.
 *
 * ## Responses
 * Since requests are broadcasted, multiple handlers on different servers may
 * receive the same request. Each handler instance may send **at most one**
 * response for a given request.
 *
 * ## Example
 * ```
 * @HandleRedisRequest
 * fun handlePlayerRequest(ctx: RequestContext<GetPlayerRequest>) {
 *     if (ctx.originatesFromThisClient()) return // ignore self-originated requests
 *     val players = loadPlayers(ctx.request)
 *     ctx.respond(PlayerListResponse(players))
 * }
 *
 * @HandleRedisRequest
 * fun handlePlayerRequestAsync(ctx: RequestContext<GetPlayerRequest>) {
 *     coroutineScope.launch {
 *         val players = fetchPlayersAsync(ctx.request)
 *         ctx.respond(PlayerListResponse(players))
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HandleRedisRequest
