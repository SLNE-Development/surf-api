package dev.slne.surf.nats.rpc

/**
 * Sets the protocol name of a service argument instead of its Kotlin parameter name.
 *
 * See [NatsService] for the compatibility rules when renaming declarations.
 *
 * @property name a non-blank argument name that does not collide with another argument's serialized name.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsParam(
    val name: String,
)
