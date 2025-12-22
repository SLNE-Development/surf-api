package de.slne.redis.request

/**
 * Marks a method as a Redis request handler.
 *
 * Methods annotated with this annotation are automatically registered
 * to handle incoming Redis requests of a specific type.
 *
 * Handler methods must:
 * - Have **exactly one parameter** of type [RequestContext]<T>
 * - `T` must be a subtype of [RedisRequest]
 * - **Not** be a `suspend` function
 * - Return `Unit`
 *
 * Request handlers are invoked **synchronously on the Redis Pub/Sub thread**.
 * If asynchronous or suspending work is required, the handler must explicitly
 * launch its own coroutine and call [RequestContext.respond] from there.
 *
 * Exactly one response may be sent per request.
 *
 * Example:
 * ```
 * @HandleRedisRequest
 * fun handlePlayerRequest(ctx: RequestContext<GetPlayerRequest>) {
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
