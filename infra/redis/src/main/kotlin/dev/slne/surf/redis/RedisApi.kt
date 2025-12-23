package dev.slne.surf.redis

import dev.slne.surf.redis.RedisApi.Companion.create
import dev.slne.surf.redis.config.InternalConfig
import dev.slne.surf.redis.event.RedisEvent
import dev.slne.surf.redis.event.RedisEventBus
import dev.slne.surf.redis.request.RedisRequest
import dev.slne.surf.redis.request.RedisResponse
import dev.slne.surf.redis.request.RequestResponseBus
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.list.SyncList
import dev.slne.surf.redis.sync.map.SyncMap
import dev.slne.surf.redis.sync.set.SyncSet
import dev.slne.surf.redis.sync.value.SyncValue
import dev.slne.surf.surfapi.core.api.serializer.SurfSerializerModule
import dev.slne.surf.surfapi.core.api.util.logger
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.jetbrains.annotations.Blocking
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Central API for managing Redis connections.
 *
 * This class owns a single [RedisClient] instance and provides
 * one shared [StatefulRedisConnection] for commands and one
 * [StatefulRedisPubSubConnection] for Pub/Sub usage.
 *
 * Instances are created via [create] and are responsible for
 * their own lifecycle.
 *
 * ## Lifecycle
 * This API uses a two-phase setup:
 * 1. Register features/listeners (e.g. via [subscribeToEvents])
 * 2. Call [freeze] to prevent further registrations
 * 3. Call [connect] to open Redis connections
 *
 * Use [freezeAndConnect] as a convenience to perform steps 2 and 3.
 */
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisApi private constructor(
    private val config: InternalConfig,
    internal val json: Json
) {

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

    val eventBus = RedisEventBus(this)
    val requestResponseBus = RequestResponseBus(this)

    private val syncStructures = ObjectArrayList<SyncStructure<*>>()
    private val syncStructureScope = CoroutineScope(
        Dispatchers.Default
                + SupervisorJob()
                + CoroutineName("surf-redis-sync-structures")
                + CoroutineExceptionHandler { context, throwable ->
            log.atSevere()
                .withCause(throwable)
                .log("Uncaught exception in Redis sync structure coroutine (context: $context)")
        }
    )

    private var frozen = false

    companion object {
        private val log = logger()

        /**
         * Creates a [RedisApi] instance from an explicit [RedisURI].
         *
         * The returned instance is not connected yet. Call [freeze] and then [connect],
         * or use [freezeAndConnect].
         *
         * @param redisURI Redis connection URI
         * @param serializerModule additional serializers to be included in the internal [Json] instance
         */
        fun create(
            redisURI: RedisURI,
            serializerModule: SerializersModule = EmptySerializersModule()
        ): RedisApi {
            val config = InternalConfig(
                host = redisURI.host,
                port = redisURI.port
            )
            val api = RedisApi(config, createJson(serializerModule))

            return api
        }

        /**
         * Creates a [RedisApi] instance using plugin configuration.
         *
         * The returned instance is not connected yet. Call [freeze] and then [connect],
         * or use [freezeAndConnect].
         *
         * @param pluginDataPath path to the plugin data directory
         * @param pluginsPath root plugins directory (used for global config)
         * @param serializerModule additional serializers to be included in the internal [Json] instance
         */
        fun create(
            pluginDataPath: Path,
            pluginsPath: Path = pluginDataPath.parent,
            serializerModule: SerializersModule = EmptySerializersModule()
        ): RedisApi {
            val config = InternalConfig.load(pluginDataPath, pluginsPath)
            val api = RedisApi(config, createJson(serializerModule))

            return api
        }

        private fun createJson(serializerModule: SerializersModule) = Json {
            serializersModule = SerializersModule {
                include(SurfSerializerModule.all)
                include(serializerModule)
            }
        }
    }

    /**
     * Initializes the Redis client and opens all required connections.
     *
     * This method is blocking and must only be called once.
     * The API must be [freeze]d before connecting to ensure all registrations are complete.
     *
     * @throws IllegalArgumentException if the API is not frozen
     * @throws IllegalArgumentException if already connected
     */
    @Blocking
    fun connect(): RedisApi = apply {
        require(isFrozen()) { "Redis client must be frozen before connecting" }
        require(!isConnected()) { "Redis client already initialized" }

        val redisURI = RedisURI.builder().apply {
            withHost(config.host)
            withPort(config.port)
            config.password?.let { withPassword(it) }
        }.build()
        redisClient = RedisClient.create(redisURI)

        connection = redisClient.connect()
        pubSubConnection = redisClient.connectPubSub()

        eventBus.init()
        requestResponseBus.init()

        for (structure in syncStructures) {
            syncStructureScope.launch {
                structure.init()
            }
        }
    }

    /**
     * Convenience method that calls [freeze] and then [connect].
     *
     * This method is blocking.
     */
    @Blocking
    fun freezeAndConnect(): RedisApi = apply {
        freeze()
        connect()
    }

    /**
     * Freezes this API instance and prevents further registrations.
     *
     * After freezing, [connect] may be called.
     *
     * @throws IllegalArgumentException if already frozen
     */
    fun freeze() {
        require(!isFrozen()) { "Redis client already frozen" }

        frozen = true
    }

    /**
     * @return true if this API instance has been frozen and no further registrations are allowed
     */
    fun isFrozen(): Boolean = frozen

    /**
     * Shuts down the Redis client and all underlying connections.
     *
     * This method is safe to call multiple times but will have no effect if not connected.
     */
    @Blocking
    fun disconnect() {
        if (!isConnected()) return
        redisClient.shutdown()

        requestResponseBus.close()
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

    /**
     * Publishes a [RedisEvent] via the internal [RedisEventBus].
     * @see RedisEventBus.publish
     */
    fun publishEvent(event: RedisEvent) = eventBus.publish(event)

    /**
     * Registers event handlers on the given listener instance.
     * @see RedisEventBus.registerListener
     */
    fun subscribeToEvents(listener: Any) = eventBus.registerListener(listener)

    /**
     * @see RequestResponseBus.sendRequest
     */
    suspend inline fun <reified T : RedisResponse> sendRequest(
        request: RedisRequest,
        timeoutMs: Long = RequestResponseBus.Companion.DEFAULT_TIMEOUT_MS
    ) = requestResponseBus.sendRequest<T>(request, timeoutMs)

    /**
     * @see RequestResponseBus.sendRequest
     */
    suspend fun <T : RedisResponse> sendRequest(
        request: RedisRequest,
        responseType: Class<T>,
        timeoutMs: Long = RequestResponseBus.Companion.DEFAULT_TIMEOUT_MS
    ) = requestResponseBus.sendRequest(request, responseType, timeoutMs)

    /**
     * @see RequestResponseBus.registerRequestHandler
     */
    fun registerRequestHandler(handler: Any) = requestResponseBus.registerRequestHandler(handler)

    /**
     * Creates a new [SyncList] instance.
     * @see SyncList
     */
    inline fun <reified E : Any> createSyncList(
        id: String,
        ttl: Duration = SyncList.Companion.DEFAULT_TTL
    ): SyncList<E> = createSyncList(id, serializer(), ttl)

    /**
     * Creates a new [SyncList] instance.
     * @see SyncList
     */
    fun <E : Any> createSyncList(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration = SyncList.Companion.DEFAULT_TTL
    ) = createSyncStructure {
        SyncList(this, id, syncStructureScope, elementSerializer, ttl)
    }

    /**
     * Creates a new [SyncSet] instance.
     * @see SyncSet
     */
    inline fun <reified E : Any> createSyncSet(
        id: String,
        ttl: Duration = SyncSet.Companion.DEFAULT_TTL
    ): SyncSet<E> = createSyncSet(id, serializer(), ttl)

    /**
     * Creates a new [SyncSet] instance.
     * @see SyncSet
     */
    fun <E : Any> createSyncSet(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration = SyncSet.Companion.DEFAULT_TTL
    ) = createSyncStructure {
        SyncSet(this, id, syncStructureScope, elementSerializer, ttl)
    }

    /**
     * Creates a new [SyncValue] instance.
     * @see SyncValue
     */
    inline fun <reified T : Any> createSyncValue(
        id: String,
        defaultValue: T,
        ttl: Duration = SyncValue.Companion.DEFAULT_TTL
    ): SyncValue<T> = createSyncValue(id, serializer(), defaultValue, ttl)

    /**
     * Creates a new [SyncValue] instance.
     * @see SyncValue
     */
    fun <T : Any> createSyncValue(
        id: String,
        serializer: KSerializer<T>,
        defaultValue: T,
        ttl: Duration = SyncValue.Companion.DEFAULT_TTL
    ) = createSyncStructure {
        SyncValue(this, id, syncStructureScope, serializer, defaultValue, ttl)
    }

    /**
     * Creates a new [SyncMap] instance.
     * @see SyncMap
     */
    inline fun <reified K : Any, reified V : Any> createSyncMap(
        id: String,
        ttl: Duration = SyncMap.Companion.DEFAULT_TTL
    ): SyncMap<K, V> = createSyncMap(id, serializer(), serializer(), ttl)

    fun <K : Any, V : Any> createSyncMap(
        id: String,
        keySerializer: KSerializer<K>,
        valueSerializer: KSerializer<V>,
        ttl: Duration = SyncMap.Companion.DEFAULT_TTL
    ) = createSyncStructure {
        SyncMap(this, id, syncStructureScope, keySerializer, valueSerializer, ttl)
    }

    /**
     * Creates a new instance of a synchronization structure using the provided creator function.
     * Ensures that the Redis client is not frozen before creating the synchronization structure.
     * The created structure is added to the internal list of sync structures managed by the Redis API.
     *
     * @param creator A factory function responsible for creating a specific type of synchronization structure.
     * @return The newly created synchronization structure of type [S].
     * @throws IllegalStateException if the Redis client is frozen when attempting to create the structure.
     */
    private inline fun <S : SyncStructure<*>> createSyncStructure(creator: () -> S): S {
        require(!isFrozen()) { "Redis client must not be frozen to create sync structures" }
        val structure = creator()
        syncStructures.add(structure)
        return structure
    }
}
