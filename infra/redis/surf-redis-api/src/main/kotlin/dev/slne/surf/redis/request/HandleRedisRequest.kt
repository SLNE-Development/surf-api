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
 * - be either a regular or a `suspend` function
 * - return `Unit`
 *
 * ## Execution model
 * Request handler methods are invoked inside a coroutine launched on the internal Redis listener
 * scope, which uses **[kotlinx.coroutines.Dispatchers.Default]**.
 *
 * Regular (non-suspend) handlers run on a `Dispatchers.Default` thread.
 * `suspend` handlers are fully supported — [RequestContext.respond] can be called at any
 * suspension point.
 *
 * Even though the handler runs in a coroutine, **do not perform blocking work directly**
 * (e.g. blocking I/O, blocking database drivers, `Thread.sleep`). Always switch the
 * dispatcher for blocking operations:
 *
 * ```
 * @HandleRedisRequest
 * suspend fun handle(ctx: RequestContext<MyRequest>) {
 *     val result = withContext(Dispatchers.IO) {
 *         loadFromDatabaseBlocking(ctx.request)
 *     }
 *     ctx.respond(MyResponse(result))
 * }
 * ```
 *
 * ## Responses
 * Since requests are broadcasted, multiple handlers on different servers may
 * receive the same request. Each handler instance may send **at most one**
 * response for a given request.
 *
 * ## Example
 * ```
 * // Regular handler
 * @HandleRedisRequest
 * fun handlePlayerRequest(ctx: RequestContext<GetPlayerRequest>) {
 *     if (ctx.originatesFromThisClient()) return
 *     val player = loadPlayer(ctx.request.playerId)
 *     ctx.respond(PlayerListResponse(player.name))
 * }
 *
 * // Suspend handler — can use suspend functions directly
 * @HandleRedisRequest
 * suspend fun handlePlayerRequestSuspend(ctx: RequestContext<GetPlayerRequest>) {
 *     if (ctx.originatesFromThisClient()) return
 *     val player = fetchPlayerSuspending(ctx.request.playerId)
 *     ctx.respond(PlayerListResponse(player.name))
 * }
 *
 * // Blocking I/O — switch to Dispatchers.IO
 * @HandleRedisRequest
 * suspend fun handlePlayerRequestBlocking(ctx: RequestContext<GetPlayerRequest>) {
 *     if (ctx.originatesFromThisClient()) return
 *     val player = withContext(Dispatchers.IO) { loadFromDatabaseBlocking(ctx.request.playerId) }
 *     ctx.respond(PlayerListResponse(player.name))
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HandleRedisRequest
