package dev.slne.surf.redis

import dev.slne.surf.redis.RedisApi.Companion.create
import dev.slne.surf.redis.cache.RedisSetIndexes
import dev.slne.surf.redis.cache.SimpleRedisCache
import dev.slne.surf.redis.cache.SimpleSetRedisCache
import dev.slne.surf.redis.credentials.RedisCredentialsProvider
import dev.slne.surf.redis.event.RedisEvent
import dev.slne.surf.redis.request.RedisRequest
import dev.slne.surf.redis.request.RedisResponse
import dev.slne.surf.redis.request.RequestResponseBus
import dev.slne.surf.redis.request.sendRequest
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.list.SyncList
import dev.slne.surf.redis.sync.map.SyncMap
import dev.slne.surf.redis.sync.set.SyncSet
import dev.slne.surf.redis.sync.value.SyncValue
import dev.slne.surf.surfapi.core.api.serializer.SurfSerializerModule
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDStringSerializer
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import io.netty.channel.epoll.Epoll
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.serializer
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.Blocking
import org.redisson.Redisson
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.redisson.api.redisnode.RedisNodes
import org.redisson.codec.BaseEventCodec
import org.redisson.config.Config
import org.redisson.config.TransportMode
import org.redisson.misc.RedisURI
import reactor.core.Disposable
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Central API for managing Redis connections.
 *
 * This class owns a single [RedissonClient] instance and provides
 * a shared [RedissonReactiveClient] for reactive command and Pub/Sub usage.
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
class RedisApi private constructor(
    private val config: Config,
    val json: Json,
) {
    /**
     * Underlying Redis client instance.
     *
     * Initialized when [connect] is called.
     */
    lateinit var redisson: RedissonClient
        private set

    lateinit var redissonReactive: RedissonReactiveClient
        private set

    var redisOsType: BaseEventCodec.OSType? = null
        private set

    val eventBus = RedisComponentProvider.get().createEventBus(this)
    val requestResponseBus = RedisComponentProvider.get().createRequestResponseBus(this)

    val clientId get() = RedisComponentProvider.get().clientId

    private val syncStructures = ObjectArrayList<SyncStructure<*>>()
    private val syncStructureScope = CoroutineScope(
        Dispatchers.Default
                + SupervisorJob()
                + CoroutineName("surf-redis-sync-structures")
                + CoroutineExceptionHandler { context, throwable ->
            log.atSevere()
                .withCause(throwable)
                .log(
                    "Uncaught exception in Redis sync structure coroutine (context: ${
                        context.toString().replace("{", "[").replace("}", "]")
                    })"
                )
        }
    )

    private val disposables = mutableObjectListOf<Disposable>()

    private var frozen = false

    companion object {
        private val log = logger()
        private val transportMode = if (Epoll.isAvailable()) {
            TransportMode.EPOLL
        } else {
            TransportMode.NIO
        }

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
            val config = Config()
                .setPassword(redisURI.password)
                .setTransportMode(transportMode)
                .setEventLoopGroup(RedisComponentProvider.get().eventLoopGroup)
                .setExecutor(RedisComponentProvider.get().redissonExecutorService)
                .apply {
                    useSingleServer()
                        .setAddress(redisURI.toString())
                }

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
        @Deprecated(
            message = "Surf Redis is no longer shaded into plugins. Therefore, plugin paths are no longer relevant.",
            replaceWith = ReplaceWith("create(serializerModule)"),
            level = DeprecationLevel.WARNING
        )
        fun create(
            pluginDataPath: Path,
            pluginsPath: Path = pluginDataPath.parent,
            serializerModule: SerializersModule = EmptySerializersModule()
        ): RedisApi {
            return create(serializerModule)
        }

        fun create(
            serializerModule: SerializersModule = EmptySerializersModule()
        ): RedisApi {
            return create(RedisCredentialsProvider.instance.redisURI(), serializerModule)
        }

        @OptIn(ExperimentalSerializationApi::class)
        private fun createJson(serializerModule: SerializersModule) = Json {
            namingStrategy = JsonNamingStrategy.SnakeCase
            encodeDefaults = true
            serializersModule = SerializersModule {
                include(SurfSerializerModule.all.overwriteWith(SerializersModule {
                    contextual(JavaUUIDStringSerializer) // UUID as string rather than byte array
                }))

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

        log.atInfo()
            .log("Connecting to Redis...")

        redisson = Redisson.create(config)
        redissonReactive = redisson.reactive()

        fetchRedisOs()

        eventBus.init()
        requestResponseBus.init()

        for (structure in syncStructures) {
            structure.init().block()
        }
    }

    @Blocking
    private fun fetchRedisOs() {
        @Language("Redis")
        val lua = """
            local info = redis.call('INFO', 'server')
            return string.match(info, 'os:([^\\r\\n]+)')
        """.trimIndent()

        val os = redisson.script.eval<String?>(
            RScript.Mode.READ_ONLY,
            lua,
            RScript.ReturnType.STRING,
        )

        if (os == null || os.contains("Windows")) {
            redisOsType = BaseEventCodec.OSType.WINDOWS
        } else if (os.contains("NONSTOP")) {
            redisOsType = BaseEventCodec.OSType.HPNONSTOP
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
        requestResponseBus.close()
        eventBus.close()

        for (structure in syncStructures) {
            structure.dispose()
        }
        syncStructureScope.cancel("RedisApi disconnected")

        for (disposable in disposables) {
            disposable.dispose()
        }
        disposables.clear()

        redisson.shutdown()
    }

    /**
     * Indicates whether the Redis client is initialized and not shutting down.
     *
     * Note that this does not guarantee Redis availability; it only
     * reflects the local connection state.
     */
    fun isConnected(): Boolean = ::redisson.isInitialized && !redisson.isShuttingDown


    /**
     * Performs an active health check against Redis.
     *
     * Sends a `PING` command and waits for a `PONG` response.
     *
     * @return true if Redis responds successfully, false otherwise
     */
    suspend fun isAlive(): Boolean = try {
        withContext(Dispatchers.IO) {
            redisson.getRedisNodes(RedisNodes.SINGLE).pingAll()
        }
    } catch (_: Exception) {
        false
    }

    /**
     * Publishes a [RedisEvent] via the internal [dev.slne.surf.redis.event.RedisEventBus].
     * @see dev.slne.surf.redis.event.RedisEventBus.publish
     */
    fun publishEvent(event: RedisEvent) = eventBus.publish(event)

    /**
     * Registers event handlers on the given listener instance.
     * @see dev.slne.surf.redis.event.RedisEventBus.registerListener
     */
    fun subscribeToEvents(listener: Any) = eventBus.registerListener(listener)

    /**
     * @see dev.slne.surf.redis.request.RequestResponseBus.sendRequest
     */
    suspend inline fun <reified T : RedisResponse> sendRequest(
        request: RedisRequest,
        timeoutMs: Long = RequestResponseBus.DEFAULT_TIMEOUT_MS
    ) = requestResponseBus.sendRequest<T>(request, timeoutMs)

    /**
     * @see dev.slne.surf.redis.request.RequestResponseBus.sendRequest
     */
    suspend fun <T : RedisResponse> sendRequest(
        request: RedisRequest,
        responseType: Class<T>,
        timeoutMs: Long = RequestResponseBus.DEFAULT_TIMEOUT_MS
    ) = requestResponseBus.sendRequest(request, responseType, timeoutMs)

    /**
     * @see dev.slne.surf.redis.request.RequestResponseBus.registerRequestHandler
     */
    fun registerRequestHandler(handler: Any) = requestResponseBus.registerRequestHandler(handler)

    /**
     * Creates a new [SyncList] instance.
     * @see SyncList
     */
    inline fun <reified E : Any> createSyncList(
        id: String,
        ttl: Duration = SyncList.DEFAULT_TTL
    ): SyncList<E> = createSyncList(id, json.serializersModule.serializer(), ttl)

    /**
     * Creates a new [SyncList] instance.
     * @see SyncList
     */
    fun <E : Any> createSyncList(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration = SyncList.DEFAULT_TTL
    ) = createSyncStructure {
        RedisComponentProvider.get().createSyncList(id, elementSerializer, ttl, this)
    }

    /**
     * Creates a new [SyncSet] instance.
     * @see SyncSet
     */
    inline fun <reified E : Any> createSyncSet(
        id: String,
        ttl: Duration = SyncSet.DEFAULT_TTL
    ): SyncSet<E> = createSyncSet(id, json.serializersModule.serializer(), ttl)

    /**
     * Creates a new [SyncSet] instance.
     * @see SyncSet
     */
    fun <E : Any> createSyncSet(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration = SyncSet.DEFAULT_TTL
    ) = createSyncStructure {
        RedisComponentProvider.get().createSyncSet(id, elementSerializer, ttl, this)
    }

    /**
     * Creates a new [SyncValue] instance.
     * @see SyncValue
     */
    inline fun <reified T : Any> createSyncValue(
        id: String,
        defaultValue: T,
        ttl: Duration = SyncValue.DEFAULT_TTL
    ): SyncValue<T> = createSyncValue(id, json.serializersModule.serializer(), defaultValue, ttl)

    /**
     * Creates a new [SyncValue] instance.
     * @see SyncValue
     */
    fun <T : Any> createSyncValue(
        id: String,
        serializer: KSerializer<T>,
        defaultValue: T,
        ttl: Duration = SyncValue.DEFAULT_TTL
    ) = createSyncStructure {
        RedisComponentProvider.get().createSyncValue(id, serializer, defaultValue, ttl, this)
    }

    /**
     * Creates a new [SyncMap] instance.
     * @see SyncMap
     */
    inline fun <reified K : Any, reified V : Any> createSyncMap(
        id: String,
        ttl: Duration = SyncMap.DEFAULT_TTL
    ): SyncMap<K, V> = createSyncMap(id, json.serializersModule.serializer(), json.serializersModule.serializer(), ttl)

    fun <K : Any, V : Any> createSyncMap(
        id: String,
        keySerializer: KSerializer<K>,
        valueSerializer: KSerializer<V>,
        ttl: Duration = SyncMap.DEFAULT_TTL
    ) = createSyncStructure {
        RedisComponentProvider.get().createSyncMap(id, keySerializer, valueSerializer, ttl, this)
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

    /**
     * Create a [SimpleRedisCache] for the given `namespace` using an automatically derived
     * serializer for value type `V`.
     *
     * This inline variant uses a reified type parameter `V`, allowing the appropriate
     * `KSerializer<V>` to be obtained via `serializer()`.
     *
     * @param namespace Prefix placed before each Redis key.
     * @param ttl Time-to-live for cache entries as a [kotlin.time.Duration].
     * @param keyToString Function that converts a key of type `K` to its string representation.
     *                    Defaults to `toString()`.
     * @return A new [SimpleRedisCache] instance backed by this [RedisApi].
     * @see SimpleRedisCache
     */
    inline fun <K : Any, reified V : Any> createSimpleCache(
        namespace: String,
        ttl: Duration,
        noinline keyToString: (K) -> String = { it.toString() }
    ): SimpleRedisCache<K, V> = createSimpleCache(namespace, json.serializersModule.serializer(), ttl, keyToString)

    /**
     * Create a [SimpleRedisCache] for the given `namespace` using the provided [KSerializer].
     *
     * This variant allows an explicit serializer for the value type `V` to be supplied.
     *
     * @param namespace Prefix placed before each Redis key.
     * @param serializer A [KSerializer] for the value type `V` used for JSON (de-)serialization.
     * @param ttl Time-to-live for cache entries as a [kotlin.time.Duration].
     * @param keyToString Function that converts a key of type `K` to its string representation.
     *                    Defaults to `toString()`.
     * @return A new [SimpleRedisCache] instance backed by this [RedisApi].
     * @see SimpleRedisCache
     */
    fun <K : Any, V : Any> createSimpleCache(
        namespace: String,
        serializer: KSerializer<V>,
        ttl: Duration,
        keyToString: (K) -> String = { it.toString() }
    ): SimpleRedisCache<K, V> {
        return RedisComponentProvider.get().createSimpleCache(namespace, serializer, ttl, keyToString, this)
    }

    fun <T : Any> createSimpleSetRedisCache(
        namespace: String,
        serializer: KSerializer<T>,
        ttl: Duration,
        idOf: (T) -> String,
        indexes: RedisSetIndexes<T> = RedisSetIndexes.empty()
    ): SimpleSetRedisCache<T> {
        return RedisComponentProvider.get().createSimpleSetRedisCache(namespace, serializer, ttl, idOf, indexes, this)
    }
}
