package dev.slne.surf.nats

import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Marks a Spring bean that implements generated surf-nats service interfaces and serves them to other clients.
 *
 * Without [value] the bean serves every generated service interface it implements. [value] narrows that to the
 * listed interfaces; each one must be a generated service interface implemented by the bean.
 *
 * The bean must be a singleton. It is registered when the plugin is enabled, and no other bean of the plugin may
 * serve the same service.
 *
 * @property value the generated service interfaces to serve.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
public annotation class NatsService(
    vararg val value: KClass<*>,
)
