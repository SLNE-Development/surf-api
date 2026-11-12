package dev.slne.surf.nats.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.api.core.config.constraints.ExistingFile
import dev.slne.surf.api.core.config.type.number.IntOr
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.NatsHandlerLimits
import dev.slne.surf.nats.runtime.ConnectRules
import dev.slne.surf.nats.runtime.RuntimeDefaults
import io.nats.client.Options
import io.nats.client.support.NatsUri
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.net.URISyntaxException
import java.nio.file.Path

/**
 * Settings of the shared NATS connection, read from `config.yml` in [NatsInstance.dataPath].
 *
 * Keys are the kebab-case property names, such as `drain-timeout-seconds`. Unset numeric values fall back to the
 * jnats reconnect wait, a drain timeout of 10 seconds and the handler limits of [NatsHandlerLimits]. The
 * `*-by-service` maps are keyed by the proto full name of the service.
 */
@InternalNatsApi
@ConfigSerializable
internal data class NatsConfig(
    val servers: List<String> = listOf(ConnectRules.DEFAULT_SERVER),
    val auth: Auth = Auth(),
    val connectionName: String? = null,
    val reconnectWaitMillis: IntOr.Default = IntOr.Default.USE_DEFAULT,
    val drainTimeoutSeconds: IntOr.Default = IntOr.Default.USE_DEFAULT,
    val maxConcurrentHandlers: IntOr.Default = IntOr.Default.USE_DEFAULT,
    val maxConcurrentHandlersByService: Map<String, Int> = emptyMap(),
    val maxConcurrentLongHandlers: IntOr.Default = IntOr.Default.USE_DEFAULT,
    val maxConcurrentLongHandlersByService: Map<String, Int> = emptyMap(),
) {

    val reconnectWait = reconnectWaitMillis or Options.DEFAULT_RECONNECT_WAIT.toMillis().toInt()
    val drainTimeout = drainTimeoutSeconds or RuntimeDefaults.DRAIN_TIMEOUT_SECONDS

    /**
     * Returns how many handlers of the service with the proto name [serviceName] may run at the same time.
     *
     * The entries of this configuration for [serviceName] take precedence over its global values, and both over
     * [defaults]. A configured total without a configured long share gets half of that total as its long share.
     */
    fun handlerLimits(serviceName: String, defaults: HandlerLimits = NatsHandlerLimits.defaults): HandlerLimits {
        return defaults.overriddenBy(
            total = maxConcurrentHandlersByService[serviceName] ?: maxConcurrentHandlers.value,
            long = maxConcurrentLongHandlersByService[serviceName] ?: maxConcurrentLongHandlers.value,
        )
    }

    /**
     * Authentication settings. At most one of [token], [username] with [password], or [credentialsFile] may be set.
     */
    @ConfigSerializable
    data class Auth(
        val token: String? = null,
        val username: String? = null,
        val password: String? = null,
        @ExistingFile val credentialsFile: Path? = null
    ) {
        fun validate() {
            ConnectRules.authProblem(token, username, password, credentialsFile?.toString())?.let(::error)
        }

        /**
         * Returns these settings with empty values unset, as [ConnectRules.authProblem] treats them.
         */
        fun withoutEmptyValues(): Auth = Auth(
            token = token?.takeUnless(String::isEmpty),
            username = username?.takeUnless(String::isEmpty),
            password = password?.takeUnless(String::isEmpty),
            credentialsFile = credentialsFile?.takeUnless { it.toString().isEmpty() },
        )

        companion object {
            /**
             * Returns the auth settings of the `SURF_NATS_*` environment variables, or `null` when none is set.
             */
            fun fromEnv(): Auth? {
                val auth = Auth(
                    token = NatsEnvironment.SURF_NATS_TOKEN,
                    username = NatsEnvironment.SURF_NATS_USERNAME,
                    password = NatsEnvironment.SURF_NATS_PASSWORD,
                    credentialsFile = NatsEnvironment.SURF_NATS_CREDENTIALS_FILE
                ).withoutEmptyValues()
                return auth.takeIf { it != Auth() }
            }
        }
    }

    fun validate() {
        check(servers.isNotEmpty()) { "NATS servers must not be empty" }
        check(drainTimeout > 0) { "NATS drain-timeout-seconds must be positive, was $drainTimeout" }
        check(reconnectWait >= 0) { "NATS reconnect-wait-millis must not be negative, was $reconnectWait" }
        validateHandlerLimits()

        for (rawServer in servers) {
            try {
                NatsUri(rawServer)
            } catch (e: URISyntaxException) {
                throw IllegalStateException("Invalid NATS server URI: $rawServer", e)
            }
        }

        auth.validate()
    }

    private fun validateHandlerLimits() {
        maxConcurrentHandlers.value?.let { limit ->
            check(limit > 0) { "NATS max-concurrent-handlers must be positive, was $limit" }
        }
        maxConcurrentLongHandlers.value?.let { limit ->
            check(limit > 0) { "NATS max-concurrent-long-handlers must be positive, was $limit" }
            val total = maxConcurrentHandlers.value ?: return@let
            check(limit <= total) {
                "NATS max-concurrent-long-handlers must not exceed max-concurrent-handlers ($total), was $limit"
            }
        }
        for ((service, limit) in maxConcurrentHandlersByService) {
            check(limit > 0) { "NATS max-concurrent-handlers-by-service of $service must be positive, was $limit" }
        }
        for ((service, limit) in maxConcurrentLongHandlersByService) {
            check(limit > 0) {
                "NATS max-concurrent-long-handlers-by-service of $service must be positive, was $limit"
            }
            val total = maxConcurrentHandlersByService[service] ?: continue
            check(limit <= total) {
                "NATS max-concurrent-long-handlers-by-service of $service must not exceed its " +
                        "max-concurrent-handlers-by-service ($total), was $limit"
            }
        }
    }

    /**
     * Returns this configuration with each set `SURF_NATS_*` environment variable in place of its key.
     *
     * Any auth variable replaces the whole [auth] block. The per-service handler limits have no variables.
     */
    fun overwriteFromEnv() = copy(
        servers = NatsEnvironment.SURF_NATS_SERVERS ?: servers,
        connectionName = NatsEnvironment.SURF_NATS_CONNECTION_NAME ?: connectionName,
        maxConcurrentHandlers = NatsEnvironment.SURF_NATS_MAX_CONCURRENT_HANDLERS?.let(IntOr::Default)
            ?: maxConcurrentHandlers,
        maxConcurrentLongHandlers = NatsEnvironment.SURF_NATS_MAX_CONCURRENT_LONG_HANDLERS?.let(IntOr::Default)
            ?: maxConcurrentLongHandlers,
        auth = (Auth.fromEnv() ?: auth).withoutEmptyValues()
    )

    companion object : SpongeYmlConfigClass<NatsConfig>(
        NatsConfig::class.java,
        NatsInstance.get().dataPath,
        "config.yml"
    )
}

@InternalNatsApi
internal val natsConfig by lazy { NatsConfig.getConfig().overwriteFromEnv().also { it.validate() } }
