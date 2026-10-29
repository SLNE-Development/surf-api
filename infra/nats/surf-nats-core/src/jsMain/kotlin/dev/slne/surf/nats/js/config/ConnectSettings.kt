package dev.slne.surf.nats.js.config

import dev.slne.surf.nats.js.api.ConnectOptions
import dev.slne.surf.nats.js.interop.requireFromJs
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.runtime.ConnectRules

/**
 * Validated [ConnectOptions], following the rules of the JVM `NatsConfig`.
 */
internal class ConnectSettings(
    val name: String,
    val servers: List<String>,
    val auth: Auth?,
    val connectTimeoutMs: Int,
) {
    sealed interface Auth {
        class Token(val token: String) : Auth
        class UserPass(val user: String, val pass: String) : Auth
        class CredsFile(val path: String) : Auth
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 5000

        fun of(options: ConnectOptions): ConnectSettings {
            val name = ProtocolNaming.requireClientName(
                requireFromJs(options.name, "NATS client name")
            )

            val timeout = options.connectTimeoutMs ?: DEFAULT_CONNECT_TIMEOUT_MS
            require(timeout > 0) { "NATS connectTimeoutMs must be positive, was $timeout" }

            return ConnectSettings(name, servers(options.servers), auth(options), timeout)
        }

        private fun servers(value: Array<String>?): List<String> {
            if (value == null) return listOf(ConnectRules.DEFAULT_SERVER)

            val servers = ConnectRules.servers(value.asList())
            require(servers.isNotEmpty()) { "NATS servers must not be empty" }

            return servers
        }

        private fun auth(options: ConnectOptions): Auth? {
            val token = options.token.orNullIfEmpty()
            val user = options.user.orNullIfEmpty()
            val pass = options.pass.orNullIfEmpty()
            val credsFile = options.credsFile.orNullIfEmpty()

            val problem = ConnectRules.authProblem(token, user, pass, credsFile)

            require(problem == null) { problem.toString() }

            return when {
                token != null -> Auth.Token(token)
                user != null -> Auth.UserPass(user, pass!!)
                credsFile != null -> Auth.CredsFile(credsFile)
                else -> null
            }
        }

        private fun String?.orNullIfEmpty(): String? = this?.takeIf { it.isNotEmpty() }
    }
}
