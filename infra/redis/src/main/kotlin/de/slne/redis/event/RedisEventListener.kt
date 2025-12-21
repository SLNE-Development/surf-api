package de.slne.redis.event

import org.springframework.stereotype.Component

/**
 * Marks a class as a Redis event listener that should be auto-registered
 * by Spring component scanning.
 * 
 * Classes annotated with @RedisEventListener will be automatically discovered
 * and registered with the RedisEventBus when using Spring.
 * 
 * Example:
 * ```kotlin
 * @RedisEventListener
 * class MyListener {
 *     @Subscribe
 *     fun onEvent(event: MyEvent) {
 *         // Handle event
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class RedisEventListener
