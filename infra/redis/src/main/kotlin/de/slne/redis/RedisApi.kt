package de.slne.redis

import de.slne.redis.RedisApi.Companion.create
import de.slne.redis.config.InternalConfig
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.reactive.awaitSingle
import org.jetbrains.annotations.Blocking
import java.nio.file.Path

/**
 * Central API for managing Redis connections.
 *
 * This class owns a single [RedisClient] instance and provides
 * one shared [StatefulRedisConnection] for commands and one
 * [StatefulRedisPubSubConnection] for Pub/Sub usage.
 *
 * Instances are created via [create] and are responsible for
 * their own lifecycle.
 */
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisApi private constructor(private val config: InternalConfig) {

    /**
     * Underlying Redis client instance.
     *
     * Initialized when [connect] is called.
     */
    lateinit var redisClient: RedisClient
        private set

    /**
     * Stateful connection for regular Redis commands.
     */
    lateinit var connection: StatefulRedisConnection<String, String>
        private set

    /**
     * Dedicated connection for Redis Pub/Sub.
     *
     * Must not be used for regular commands.
     */
    lateinit var pubSubConnection: StatefulRedisPubSubConnection<String, String>
        private set

    companion object {

        /**
         * Creates a [RedisApi] instance from an explicit [RedisURI].
         *
         * @param redisURI Redis connection URI
         * @param connect whether [connect] should be called immediately
         */
        fun create(
            redisURI: RedisURI,
            connect: Boolean = true
        ): RedisApi {
            val config = InternalConfig(
                host = redisURI.host,
                port = redisURI.port
            )
            val api = RedisApi(config)

            if (connect) {
                api.connect()
            }

            return api
        }

        /**
         * Creates a [RedisApi] instance using plugin configuration.
         *
         * Configuration is resolved via local and optional global config.
         *
         * @param pluginDataPath path to the plugin data directory
         * @param pluginsPath root plugins directory (used for global config)
         * @param connect whether [connect] should be called immediately
         */
        fun create(
            pluginDataPath: Path,
            pluginsPath: Path = pluginDataPath.parent,
            connect: Boolean = true
        ): RedisApi {
            val config = InternalConfig.load(pluginDataPath, pluginsPath)
            val api = RedisApi(config)

            if (connect) {
                api.connect()
            }

            return api
        }
    }

    /**
     * Initializes the Redis client and opens all required connections.
     *
     * This method is blocking and must only be called once.
     *
     * @throws IllegalArgumentException if already connected
     */
    @Blocking
    fun connect(): RedisApi = apply {
        require(!isConnected()) { "Redis client already initialized" }
        val redisURI = RedisURI.create(config.host, config.port)
        redisClient = RedisClient.create(redisURI)

        connection = redisClient.connect()
        pubSubConnection = redisClient.connectPubSub()
    }

    /**
     * Shuts down the Redis client and all underlying connections.
     *
     * This method is safe to call multiple times but will have no effect if not connected.
     */
    @Blocking
    fun disconnect() {
        if (!isConnected()) return
        redisClient.shutdown()
    }

    /**
     * @return configured Redis host
     */
    fun getHost(): String = config.host

    /**
     * @return configured Redis port
     */
    fun getPort(): Int = config.port

    /**
     * Indicates whether at least one Redis connection is currently open.
     *
     * This returns `true` if either the regular command connection
     * or the Pub/Sub connection is initialized and open.
     *
     * Note that this does not guarantee Redis availability; it only
     * reflects the local connection state.
     */
    fun isConnected(): Boolean =
        (::connection.isInitialized && connection.isOpen) || (::pubSubConnection.isInitialized && pubSubConnection.isOpen)


    /**
     * Performs an active health check against Redis.
     *
     * Sends a `PING` command and waits for a `PONG` response.
     *
     * @return true if Redis responds successfully, false otherwise
     */
    suspend fun isAlive() = try {
        connection.reactive().ping().awaitSingle() == "PONG"
    } catch (_: Exception) {
        false
    }
}
