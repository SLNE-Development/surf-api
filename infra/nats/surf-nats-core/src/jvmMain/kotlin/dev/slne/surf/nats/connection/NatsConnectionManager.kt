package dev.slne.surf.nats.connection

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.config.natsConfig
import dev.slne.surf.nats.runtime.ConnectRules
import dev.slne.surf.nats.runtime.randomId
import io.nats.client.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.jetbrains.annotations.Blocking
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock

@InternalNatsApi
internal object NatsConnectionManager {

    private val log = logger()

    val instanceId: String = randomId()

    @Volatile
    private var connection: Connection? = null

    @Volatile
    private var lastConnectedUrl: String? = null

    private val flushLock = ReentrantLock(true)

    @Blocking
    fun connect() {
        if (connection != null) return

        synchronized(this) {
            if (connection != null) return

            val options = Options.builder()
                .servers(natsConfig.servers.toTypedArray())
                .connectionName(natsConfig.connectionName ?: "surf-nats-$instanceId")
                .reconnectWait(Duration.ofMillis(natsConfig.reconnectWait.toLong()))
                .maxReconnects(ConnectRules.MAX_RECONNECTS)
                .reportNoResponders()
                .dontForceFlushOnRequest()
                .pingInterval(Duration.ofSeconds(ConnectRules.PING_INTERVAL_SECONDS.toLong()))
                .advancedRequestBehavior()
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
                                .withCause(ioException)
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

    /**
     * Flushes the shared connection and fails with a [TimeoutException] after [timeout], including the wait for
     * the flush lock.
     */
    suspend fun flush(timeout: Duration) {
        runInterruptible(Dispatchers.IO) {
            withFlushLock(timeout) { remaining -> connection().flush(remaining) }
        }
    }

    /**
     * Runs [block] under the flush lock of the shared connection and passes it the part of [timeout] that is left.
     *
     * Every operation that pings the server and waits for the pong, such as `Connection.flush` or
     * `Dispatcher.drain`, must run under this lock: jnats drops the connection of all clients when more pings than
     * `maxPingsOut` are outstanding. The lock is fair. Throws a [TimeoutException] when the lock is not acquired
     * within [timeout].
     */
    @Blocking
    fun <T> withFlushLock(timeout: Duration, block: (remaining: Duration) -> T): T {
        val deadline = System.nanoTime() + timeout.toNanos()
        if (!flushLock.tryLock(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
            throw TimeoutException("Timed out after $timeout waiting for the NATS flush lock")
        }
        try {
            val remaining = deadline - System.nanoTime()
            // jnats treats a zero timeout as no timeout
            if (remaining <= 0) throw TimeoutException("No time left of $timeout after acquiring the NATS flush lock")
            return block(Duration.ofNanos(remaining))
        } finally {
            flushLock.unlock()
        }
    }

    fun health(): NatsConnectionHealth? {
        val connection = connection ?: return null
        val status = connection.status

        return NatsConnectionHealth(
            connectionName = connection.options.connectionName,
            connected = status == Connection.Status.CONNECTED,
            status = status.name,
        )
    }
}