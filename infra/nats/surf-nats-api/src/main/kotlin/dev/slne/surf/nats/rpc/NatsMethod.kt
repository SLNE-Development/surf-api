package dev.slne.surf.nats.rpc

/**
 * Sets the protocol name of a service method.
 *
 * The name must be a non-empty subject token without wildcards or whitespace and must not start
 * with `$`. Names must be unique within the service. See [NatsService] for compatibility rules
 * when renaming declarations.
 *
 * @property name the explicit method name, or the kebab-case Kotlin method name when empty.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsMethod(
    val name: String = "",
)
