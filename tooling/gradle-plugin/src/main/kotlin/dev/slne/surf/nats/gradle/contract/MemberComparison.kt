package dev.slne.surf.nats.gradle.contract

/**
 * How the generated `equals` compares a member when its class needs content-aware members.
 */
internal enum class MemberComparison {
    CONTENT,
    EQUALS,
    BITS,
    NULLABLE_BITS
}
