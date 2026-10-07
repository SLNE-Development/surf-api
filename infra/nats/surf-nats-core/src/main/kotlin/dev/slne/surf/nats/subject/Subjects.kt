package dev.slne.surf.nats.subject

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
object Subjects {

    private val WORD_BOUNDARY = "(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])".toRegex()

    private val SUBJECT = """^[^\s.*>]+(\.[^\s.*>]+)*$""".toRegex()

    internal fun kebabCase(name: String): String {
        return name.split(WORD_BOUNDARY).joinToString("-") { it.lowercase() }
    }

    internal fun requireValidSubject(subject: String, source: Any): String {
        require(SUBJECT.matches(subject)) { "Invalid NATS subject '$subject' for $source" }
        return subject
    }
}