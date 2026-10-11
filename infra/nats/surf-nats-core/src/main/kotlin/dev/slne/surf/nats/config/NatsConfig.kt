package dev.slne.surf.nats.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.api.core.config.constraints.ExistingFile
import dev.slne.surf.api.core.config.type.number.IntOr
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsInstance
import io.nats.client.Options
import io.nats.client.support.NatsUri
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.net.URISyntaxException
import java.nio.file.Path

@InternalNatsApi
@ConfigSerializable
internal data class NatsConfig(
    val servers: List<String> = listOf(Options.DEFAULT_URL),
    val auth: Auth = Auth(),
    val connectionName: String? = null,
    val reconnectWaitMillis: IntOr.Default = IntOr.Default.USE_DEFAULT,
    val drainTimeoutSeconds: IntOr.Default = IntOr.Default.USE_DEFAULT
) {

    val reconnectWait = reconnectWaitMillis or Options.DEFAULT_RECONNECT_WAIT.toMillis().toInt()
    val drainTimeout = drainTimeoutSeconds or 10

    @ConfigSerializable
    data class Auth(
        val token: String? = null,
        val username: String? = null,
        val password: String? = null,
        @ExistingFile val credentialsFile: Path? = null
    ) {
        fun validate() {
            check((username == null) == (password == null)) {
                "NATS auth: username and password must be set together"
            }

            val methods = listOfNotNull(token, username, credentialsFile).size
            check(methods <= 1) {
                "NATS auth: only one of token, username/password or credentials-file may be set"
            }
        }

        companion object {
            fun fromEnv(): Auth? {
                val auth = Auth(
                    token = NatsEnvironment.SURF_NATS_TOKEN,
                    username = NatsEnvironment.SURF_NATS_USERNAME,
                    password = NatsEnvironment.SURF_NATS_PASSWORD,
                    credentialsFile = NatsEnvironment.SURF_NATS_CREDENTIALS_FILE
                )
                return auth.takeIf { it != Auth() }
            }
        }
    }

    fun validate() {
        check(servers.isNotEmpty()) { "NATS servers must not be empty" }
        check(drainTimeout > 0) { "NATS drain-timeout-seconds must be positive, was $drainTimeout" }
        check(reconnectWait >= 0) { "NATS reconnect-wait-millis must not be negative, was $reconnectWait" }

        for (rawServer in servers) {
            try {
                NatsUri(rawServer)
            } catch (e: URISyntaxException) {
                throw IllegalStateException("Invalid NATS server URI: $rawServer", e)
            }
        }

        auth.validate()
    }

    fun overwriteFromEnv() = copy(
        servers = NatsEnvironment.SURF_NATS_SERVERS ?: servers,
        connectionName = NatsEnvironment.SURF_NATS_CONNECTION_NAME ?: connectionName,
        auth = Auth.fromEnv() ?: auth
    )

    companion object : SpongeYmlConfigClass<NatsConfig>(
        NatsConfig::class.java,
        NatsInstance.get().dataPath,
        "config.yml"
    )
}

@InternalNatsApi
internal val natsConfig by lazy { NatsConfig.getConfig().overwriteFromEnv().also { it.validate() } }
