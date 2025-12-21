package de.slne.redis.request

/**
 * Annotation to mark methods as Redis request handlers.
 * Methods annotated with this will automatically handle requests and send responses.
 * 
 * The method must:
 * - Take exactly one parameter of type RedisRequest
 * - Return a RedisResponse
 * - Can be a suspend function for async operations
 * 
 * Example:
 * ```
 * @RequestHandler
 * suspend fun handlePlayerRequest(request: GetPlayerRequest): PlayerListResponse {
 *     // Handle the request and return a response
 *     return PlayerListResponse(listOf("Player1", "Player2"))
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequestHandler
