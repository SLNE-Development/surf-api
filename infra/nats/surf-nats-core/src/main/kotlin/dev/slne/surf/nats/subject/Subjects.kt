package dev.slne.surf.nats.subject

import dev.slne.surf.nats.InternalNatsApi

@InternalNatsApi
internal object Subjects {

    private val WORD_BOUNDARY = "(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])".toRegex()

    private val SUBJECT = """^[^\s.*>]+(\.[^\s.*>]+)*$""".toRegex()

    internal fun kebabCase(name: String): String {
        return name.split(WORD_BOUNDARY).joinToString("-") { it.lowercase() }
    }

    internal fun requireValidSubject(subject: String, source: Any): String {
        require(SUBJECT.matches(subject)) { "Invalid NATS subject '$subject' for $source" }
        return subject
    }

    internal fun requireValidEventSubject(subject: String, source: Any): String {
        requireValidSubject(subject, source)
        require(subject.split('.').none { it.startsWith('$') }) {
            "Invalid event subject '$subject' for $source: tokens starting with '$' are reserved"
        }
        return subject
    }
}