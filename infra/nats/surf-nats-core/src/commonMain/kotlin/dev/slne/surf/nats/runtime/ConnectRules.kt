package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi

/**
 * Connection settings rules of the JVM `NatsConfig` and the JS `connect` options.
 */
@InternalNatsApi
internal object ConnectRules {
    const val DEFAULT_SERVER = "nats://localhost:4222"
    const val PING_INTERVAL_SECONDS = 10
    const val MAX_RECONNECTS = -1

    /**
     * Returns the trimmed, non-empty server URLs of [entries].
     */
    fun servers(entries: Iterable<String>): List<String> = entries.map(String::trim).filter(String::isNotEmpty)

    /**
     * Returns the server URLs of a comma-separated list.
     */
    fun parseServers(raw: String): List<String> = servers(raw.split(','))

    /**
     * Returns why the auth settings conflict, or `null`; an empty value counts as unset.
     */
    fun authProblem(token: String?, user: String?, pass: String?, credsFile: String?): String? = when {
        user.isNullOrEmpty() != pass.isNullOrEmpty() -> "NATS auth: username and password must be set together"
        listOf(token, user, credsFile).count { !it.isNullOrEmpty() } > 1 ->
            "NATS auth: only one of token, username/password or credentials-file may be set"

        else -> null
    }
}
