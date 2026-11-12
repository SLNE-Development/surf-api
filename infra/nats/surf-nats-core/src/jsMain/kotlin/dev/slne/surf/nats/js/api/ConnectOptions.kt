@file:OptIn(ExperimentalJsExport::class)

package dev.slne.surf.nats.js.api

import kotlinx.js.JsPlainObject

/**
 * Settings of `connect`.
 *
 * Set at most one way to authenticate: [token], [user] with [pass], or [credsFile]. Empty strings count as unset.
 * The client reads no environment variables.
 */
@JsExport
@JsPlainObject
external interface ConnectOptions {
    /**
     * Your own client name in kebab-case: groups of lowercase letters and digits separated by `-`.
     *
     * The client sends `<name>@<instanceId>` as the origin of its messages and as the connection name, and
     * `ANY_INSTANCE` listeners of clients with the same name share the events.
     */
    val name: String

    /**
     * The server URLs, as an array of strings or as one comma-separated string. Blank entries are ignored.
     *
     * Defaults to `nats://localhost:4222` when absent. A value without any URL or of another type is rejected.
     */
    val servers: Any?

    /**
     * The token for token authentication.
     */
    val token: String?

    /**
     * The user name for user and password authentication. Requires [pass].
     */
    val user: String?

    /**
     * The password for user and password authentication. Requires [user].
     */
    val pass: String?

    /**
     * The path of a NATS `.creds` file, read once while connecting.
     */
    val credsFile: String?

    /**
     * The timeout of the initial connection in milliseconds. Must be positive. Defaults to 5000.
     */
    val connectTimeoutMs: Int?
}
