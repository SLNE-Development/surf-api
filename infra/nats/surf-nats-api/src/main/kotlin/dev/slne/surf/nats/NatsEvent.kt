package dev.slne.surf.nats

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable

/**
 * You only need to change any of the default values if you're planning to exchange the events
 * with a different platform. For example, with a web application.
 */
@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NatsEvent(
    /**
     * If left empty, the class name in kebab-case format will be used
     */
    val subject: String = "",
    val format: WireFormat = WireFormat.CBOR,
    val prefixed: Boolean = true,
)