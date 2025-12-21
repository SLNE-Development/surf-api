package de.slne.redis.event

/**
 * Annotation to mark methods as Redis event handlers.
 * Methods annotated with this will automatically receive events matching their parameter type.
 * 
 * Example:
 * ```
 * @Subscribe
 * fun onPlayerJoin(event: PlayerJoinEvent) {
 *     // Handle the event
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe
