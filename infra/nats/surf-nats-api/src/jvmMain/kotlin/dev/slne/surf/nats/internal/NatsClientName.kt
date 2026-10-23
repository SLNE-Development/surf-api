package dev.slne.surf.nats.internal

import dev.slne.surf.nats.InternalNatsApi
import org.intellij.lang.annotations.Language
import org.intellij.lang.annotations.Pattern

@InternalNatsApi
@Language("RegExp")
public const val NATS_CLIENT_NAME_PATTERN: String = "^[a-z0-9]+(-[a-z0-9]+)*$"

@InternalNatsApi
@Pattern(NATS_CLIENT_NAME_PATTERN)
@Target(AnnotationTarget.VALUE_PARAMETER)
public annotation class NatsClientName
