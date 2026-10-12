package dev.slne.surf.nats.connection

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.config.natsConfig
import io.nats.client.*
import org.jetbrains.annotations.Blocking
import java.io.IOException
import java.time.Duration
import java.util.*

@InternalNatsApi
internal object NatsConnectionManager {

    private val log = logger()

    val instanceId: UUID = UUID.randomUUID()
    val instanceIdString = instanceId.toString()

    @Volatile
    private var connection: Connection? = null

    @Volatile
    private var lastConnectedUrl: String? = null

    @Blocking
    fun connect() {
        if (connection != null) return

        synchronized(this) {
            if (connection != null) return

            val options = Options.builder()
                .servers(natsConfig.servers.toTypedArray())
                .connectionName(natsConfig.connectionName ?: "surf-nats-${instanceIdString}")
                .reconnectWait(Duration.ofMillis(natsConfig.reconnectWait.toLong()))
                .maxReconnects(-1)
                .reportNoResponders()
                .dontForceFlushOnRequest()
                .connectionListener { connection, events ->
                    val url =
                        connection.connectedUrl?.also { lastConnectedUrl = it } ?: lastConnectedUrl
                    val at = if (events == ConnectionListener.Events.DISCONNECTED) {
                        log.atWarning()
                    } else {
                        log.atInfo()
                    }

                    at.log("NATS connection %s: %s", events.event, url)
                }
                .errorListener(object : ErrorListener {
                    override fun errorOccurred(conn: Connection, error: String) {
                        log.atWarning()
                            .log("NATS error: %s", error)
                    }

                    override fun exceptionOccurred(conn: Connection, exp: Exception) {
                        val ioException = exp as? IOException ?: exp.cause as? IOException
                        if (ioException != null) {
                            log.atWarning()
                                .log("NATS I/O error: %s", ioException.message)
                        } else {
                            log.atWarning()
                                .withCause(exp)
                                .log("NATS exception")
                        }
                    }

                    override fun slowConsumerDetected(conn: Connection, consumer: Consumer) {
                        log.atWarning()
                            .log("NATS slow consumer: %s", consumer)
                    }
                })
                .apply {
                    val auth = natsConfig.auth
                    when {
                        auth.token != null -> token(auth.token.toCharArray())
                        auth.username != null -> userInfo(auth.username, auth.password!!)
                        auth.credentialsFile != null -> credentialPath(auth.credentialsFile.toString())
                    }
                }
                .build()

            this.connection = Nats.connect(options)
        }
    }

    @Blocking
    fun disconnect() {
        synchronized(this) {
            val connection = connection ?: return
            this.connection = null

            val timeout = Duration.ofSeconds(natsConfig.drainTimeout.toLong())

            log.atInfo()
                .log("Disconnecting from NATS (drain timeout %ds)", timeout.toSeconds())

            try {
                if (!connection.drain(timeout).get()) connection.close()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                connection.close()
            } catch (e: Exception) {
                log.atWarning()
                    .withCause(e)
                    .log("Failed to disconnect from NATS, still closing connection...")
                connection.close()
            }
        }
    }

    fun connection() = checkNotNull(connection) { "NATS is not connected" }
}