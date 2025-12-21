package de.slne.redis.request

/**
 * Annotation to mark methods as Redis request handlers.
 * Methods annotated with this will automatically handle requests.
 * 
 * The method must:
 * - Take exactly one parameter of type RequestContext<TRequest>
 * - Return void (Unit)
 * - Use context.respond() to send the response (can be called from within a launched coroutine if needed)
 * 
 * Example:
 * ```
 * @RequestHandler
 * fun handlePlayerRequest(context: RequestContext<GetPlayerRequest>) {
 *     // Respond synchronously
 *     runBlocking {
 *         context.respond(PlayerListResponse(listOf("Player1", "Player2")))
 *     }
 * }
 * 
 * @RequestHandler
 * fun handlePlayerRequestAsync(context: RequestContext<GetPlayerRequest>) {
 *     // Launch coroutine to respond asynchronously
 *     context.coroutineScope.launch {
 *         val players = fetchPlayersAsync(context.request.minLevel)
 *         context.respond(PlayerListResponse(players))
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequestHandler
