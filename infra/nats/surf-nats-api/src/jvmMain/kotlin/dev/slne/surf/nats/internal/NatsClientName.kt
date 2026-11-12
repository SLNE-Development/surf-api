package dev.slne.surf.nats.internal

import dev.slne.surf.nats.InternalNatsApi
import org.intellij.lang.annotations.Language
import org.intellij.lang.annotations.Pattern

/**
 * The pattern of a client name: lowercase alphanumeric groups separated by single hyphens.
 */
@InternalNatsApi
@Language("RegExp")
public const val NATS_CLIENT_NAME_PATTERN: String = "^[a-z0-9]+(-[a-z0-9]+)*$"

/**
 * Marks a parameter that takes a client name, so that the IDE checks literals against [NATS_CLIENT_NAME_PATTERN].
 */
@InternalNatsApi
@Pattern(NATS_CLIENT_NAME_PATTERN)
@Target(AnnotationTarget.VALUE_PARAMETER)
public annotation class NatsClientName
