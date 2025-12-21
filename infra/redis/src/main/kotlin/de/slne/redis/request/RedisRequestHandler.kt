package de.slne.redis.request

import org.springframework.stereotype.Component

/**
 * Marks a class as a Redis request handler that should be auto-registered
 * by Spring component scanning.
 * 
 * Classes annotated with @RedisRequestHandler will be automatically discovered
 * and registered with the RequestResponseBus when using Spring.
 * 
 * Example:
 * ```kotlin
 * @RedisRequestHandler
 * class MyRequestHandler {
 *     @RequestHandler
 *     fun handleRequest(context: RequestContext<MyRequest>) {
 *         // Handle request
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class RedisRequestHandler
