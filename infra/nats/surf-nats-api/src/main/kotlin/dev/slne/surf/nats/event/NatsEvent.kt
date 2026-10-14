package dev.slne.surf.nats.event

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable

/**
 * Marks a class as a serializable event.
 *
 * A separate [kotlinx.serialization.Serializable] annotation is not required when the Kotlin
 * serialization compiler plugin is enabled, because this annotation uses [MetaSerializable].
 * The derived subject is part of the protocol and changes when the class is renamed. Set [subject]
 * explicitly for shared or long-lived events.
 *
 * Subjects consist of non-empty dot-separated tokens, with no wildcards or whitespace. No token
 * may start with `$`. Classes without a simple name require an explicit subject.
 *
 * @property subject the event subject, or the kebab-case simple class name when empty.
 * @property global whether events are shared across applications without client-name scoping.
 * When false, publishers and listeners must use the same client name. For global subjects, avoid
 * using a client name as the first token to prevent collisions with client-scoped events. This
 * restriction is not validated.
 */
@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsEvent(
    val subject: String = "",
    val global: Boolean = false,
)