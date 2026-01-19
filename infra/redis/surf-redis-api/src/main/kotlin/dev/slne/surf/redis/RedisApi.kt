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
import dev.slne.surf.redis.request.RequestTimeoutException
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
import org.redisson.config.EqualJitterDelay
import org.redisson.config.TransportMode
import org.redisson.misc.RedisURI
import reactor.core.Disposable
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Central entry point for surf-redis.
 *
 * `RedisApi` owns the underlying Redisson clients and wires up the higher-level surf-redis features:
 * - event distribution via [eventBus]
 * - request/response messaging via [requestResponseBus]
 * - replicated in-memory data structures ([SyncList], [SyncSet], [SyncMap], [SyncValue])
 * - simple cache helpers ([SimpleRedisCache], [SimpleSetRedisCache])
 *
 * Instances are created via [create] and manage their own lifecycle.
 *
 * ## Lifecycle
 * This API follows a two-phase setup:
 * 1. Create an instance via [create]
 * 2. Register listeners/handlers and create sync structures/caches
 * 3. Call [freeze] to lock configuration
 * 4. Call [connect] to initialize clients and start all registered features
 *
 * Use [freezeAndConnect] as a convenience for steps 3 and 4.
 *
 * Call [disconnect] to shut down all clients and dispose resources.
 *
 * ## Usage
 * A common setup is to provide a single, platform-owned [RedisApi] instance via a service and expose
 * it to consumers. Registrations (listeners / handlers) are done before connecting; consumers then
 * create sync structures where they are needed.
 *
 * ```
 * // Service that owns the RedisApi instance
 * abstract class RedisService {
 *     val redisApi = RedisApi.create()
 *
 *     fun connect() {
 *         register()
 *         redisApi.freezeAndConnect()
 *     }
 *
 *     @MustBeInvokedByOverriders
 *     @ApiStatus.OverrideOnly
 *     protected open fun register() {
 *         // Register listeners / request handlers here
 *         // redisApi.subscribeToEvents(SomeListener())
 *         // redisApi.registerRequestHandler(SomeHandler())
 *         //
 *         // Can be overridden by platform implementations to register platform-specific listeners.
 *     }
 *
 *     fun disconnect() {
 *         redisApi.disconnect()
 *     }
 *
 *     companion object {
 *         val instance = requiredService<RedisService>()
 *         fun get() = instance
 *
 *         fun publish(event: RedisEvent) = instance.redisApi.publishEvent(event)
 *
 *         fun namespaced(suffix: String) = "example-namespace:$suffix"
 *     }
 * }
 *
 * // Convenience accessor used by consumers
 * val redisApi get() = RedisService.get().redisApi
 *
 * // Platform implementation discovered/registered via AutoService
 * @AutoService(RedisService::class)
 * class PaperRedisService : RedisService() {
 *     override fun register() {
 *         super.register()
 *         // redisApi.subscribeToEvents(SomePaperListener())
 *     }
 * }
 *
 * // Consumer creates sync structures at the call site where they are needed
 * object SomeOtherService {
 *     private val someSyncList =
 *         redisApi.createSyncList<String>(RedisService.namespaced("some-sync-list"))
 * }
 * ```
 */
class RedisApi private constructor(
    private val config: Config,
    /** JSON instance used internally for (de-)serialization. */
    val json: Json,
) {
    /**
     * Underlying Redisson client.
     *
     * Initialized by [connect]. Accessing this property before [connect] will fail.
     */
    lateinit var redisson: RedissonClient
        private set

    /**
     * Reactive Redisson client, derived from [redisson] during [connect].
     *
     * Intended for reactive command and Pub/Sub usage in internal components.
     */
    lateinit var redissonReactive: RedissonReactiveClient
        private set

    /**
     * Redis OS type as reported by `INFO server` (used for Redisson codec behavior).
     *
     * This is populated during [connect]. It may remain `null` if no special handling is required.
     */
    var redisOsType: BaseEventCodec.OSType? = null
        private set

    /**
     * Event bus instance for publishing and subscribing to [RedisEvent]s.
     *
     * The bus is initialized during [connect] and closed during [disconnect].
     */
    val eventBus = RedisComponentProvider.get().createEventBus(this)

    /**
     * Request/response bus used for sending [RedisRequest]s and receiving [RedisResponse]s.
     *
     * The bus is initialized during [connect] and closed during [disconnect].
     */
    val requestResponseBus = RedisComponentProvider.get().createRequestResponseBus(this)

    /**
     * Identifier of the current client/node as provided by the component provider.
     */
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
         * @param redisURI Redis connection URI.
         * @param serializerModule Additional serializers to be included in the internal [Json] instance.
         */
        fun create(
            redisURI: RedisURI,
            serializerModule: SerializersModule = EmptySerializersModule()
        ): RedisApi {
            val config = Config()
                .setPassword(redisURI.password)
                .setTransportMode(transportMode)
                .setTcpKeepAlive(true)
                .setTcpKeepAliveInterval(5.seconds.inWholeMilliseconds.toInt())
                .setEventLoopGroup(RedisComponentProvider.get().eventLoopGroup)
                .setExecutor(RedisComponentProvider.get().redissonExecutorService)
                .apply {
                    useSingleServer()
                        .setPingConnectionInterval(10.seconds.inWholeMilliseconds.toInt())
                        .setConnectTimeout(5.seconds.inWholeMilliseconds.toInt())
                        .setRetryAttempts(10)
                        .setRetryDelay(EqualJitterDelay(200.milliseconds.toJavaDuration(), 1.seconds.toJavaDuration()))
                        .setAddress(redisURI.toString())
                }

            val api = RedisApi(config, createJson(serializerModule))
            return api
        }

        /**
         * Creates a [RedisApi] instance using plugin configuration.
         *
         * @deprecated Surf Redis is no longer shaded into plugins. Paths are no longer relevant.
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

        /**
         * Creates a [RedisApi] instance using [RedisCredentialsProvider].
         *
         * The returned instance is not connected yet. Call [freeze] and then [connect],
         * or use [freezeAndConnect].
         *
         * @param serializerModule Additional serializers to be included in the internal [Json] instance.
         */
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
     * Initializes the Redis clients and starts all registered features.
     *
     * This method is blocking and must only be called once.
     * The API must be [freeze]d before connecting to ensure registrations are complete.
     *
     * During connection:
     * - [redisson] / [redissonReactive] are created
     * - [redisOsType] may be detected
     * - [eventBus] and [requestResponseBus] are initialized
     * - all previously created sync structures are initialized
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
     * After freezing:
     * - creating sync structures via `createSync*` is no longer allowed
     * - [connect] may be called
     *
     * @throws IllegalArgumentException if already frozen
     */
    fun freeze() {
        require(!isFrozen()) { "Redis client already frozen" }

        frozen = true
    }

    /**
     * @return `true` if this API instance has been frozen and no further registrations are allowed.
     */
    fun isFrozen(): Boolean = frozen

    /**
     * Shuts down the Redis clients and disposes all resources created by this API.
     *
     * This method is safe to call multiple times; if not connected it has no effect.
     *
     * On disconnect:
     * - request/response and event buses are closed
     * - all sync structures are disposed
     * - internal coroutine scope is cancelled
     * - reactive disposables are disposed
     * - Redisson is shut down
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
     * This does not guarantee Redis availability; it only reflects the local client state.
     */
    fun isConnected(): Boolean = ::redisson.isInitialized && !redisson.isShuttingDown

    /**
     * Performs an active health check against Redis.
     *
     * Sends a `PING` and waits for the response.
     *
     * @return `true` if Redis responds successfully, `false` otherwise.
     */
    suspend fun isAlive(): Boolean = try {
        withContext(Dispatchers.IO) {
            redisson.getRedisNodes(RedisNodes.SINGLE).pingAll()
        }
    } catch (_: Exception) {
        false
    }

    /**
     * Publishes a [RedisEvent] via the internal event bus.
     *
     * @see dev.slne.surf.redis.event.RedisEventBus.publish
     */
    fun publishEvent(event: RedisEvent) = eventBus.publish(event)

    /**
     * Registers event handlers on the given listener instance.
     *
     * The listener is processed by the event bus; handler method requirements are defined
     * by the event bus annotations/contract.
     *
     * @see dev.slne.surf.redis.event.RedisEventBus.registerListener
     * @see dev.slne.surf.redis.event.OnRedisEvent
     */
    fun subscribeToEvents(listener: Any) = eventBus.registerListener(listener)

    /**
     * Sends a [RedisRequest] and awaits a [RedisResponse] of type [T].
     *
     * @param request Request instance to send.
     * @param timeoutMs Timeout in milliseconds.
     * @see dev.slne.surf.redis.request.RequestResponseBus.sendRequest
     */
    @Throws(RequestTimeoutException::class)
    suspend inline fun <reified T : RedisResponse> sendRequest(
        request: RedisRequest,
        timeoutMs: Long = RequestResponseBus.DEFAULT_TIMEOUT_MS
    ) = requestResponseBus.sendRequest<T>(request, timeoutMs)

    /**
     * Sends a [RedisRequest] and awaits a [RedisResponse] of the given [responseType].
     *
     * @param request Request instance to send.
     * @param responseType Expected response type.
     * @param timeoutMs Timeout in milliseconds.
     * @see dev.slne.surf.redis.request.RequestResponseBus.sendRequest
     */
    @Throws(RequestTimeoutException::class)
    suspend fun <T : RedisResponse> sendRequest(
        request: RedisRequest,
        responseType: Class<T>,
        timeoutMs: Long = RequestResponseBus.DEFAULT_TIMEOUT_MS
    ) = requestResponseBus.sendRequest(request, responseType, timeoutMs)

    /**
     * Registers request handlers on the given handler instance.
     *
     * @see dev.slne.surf.redis.request.RequestResponseBus.registerRequestHandler
     */
    fun registerRequestHandler(handler: Any) = requestResponseBus.registerRequestHandler(handler)


    /**
     * Creates a new [SyncList] instance identified by [id].
     *
     * Must be called before [freeze].
     *
     * @param id Logical identifier used by the structure (typically part of the Redis key namespace).
     * @param ttl Time-to-live used by the structure implementation.
     */
    inline fun <reified E : Any> createSyncList(
        id: String,
        ttl: Duration = SyncList.DEFAULT_TTL
    ): SyncList<E> = createSyncList(id, json.serializersModule.serializer(), ttl)

    /**
     * Creates a new [SyncList] instance identified by [id].
     *
     * Must be called before [freeze].
     *
     * @param id Logical identifier used by the structure (typically part of the Redis key namespace).
     * @param elementSerializer Serializer used for elements.
     * @param ttl Time-to-live used by the structure implementation.
     */
    fun <E : Any> createSyncList(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration = SyncList.DEFAULT_TTL
    ) = createSyncStructure {
        RedisComponentProvider.get().createSyncList(id, elementSerializer, ttl, this)
    }

    /**
     * Creates a new [SyncSet] instance identified by [id].
     *
     * Must be called before [freeze].
     */
    inline fun <reified E : Any> createSyncSet(
        id: String,
        ttl: Duration = SyncSet.DEFAULT_TTL
    ): SyncSet<E> = createSyncSet(id, json.serializersModule.serializer(), ttl)

    /**
     * Creates a new [SyncSet] instance identified by [id].
     *
     * Must be called before [freeze].
     */
    fun <E : Any> createSyncSet(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration = SyncSet.DEFAULT_TTL
    ) = createSyncStructure {
        RedisComponentProvider.get().createSyncSet(id, elementSerializer, ttl, this)
    }

    /**
     * Creates a new [SyncValue] instance identified by [id].
     *
     * Must be called before [freeze].
     *
     * @param defaultValue Initial/default value used by the implementation.
     */
    inline fun <reified T : Any> createSyncValue(
        id: String,
        defaultValue: T,
        ttl: Duration = SyncValue.DEFAULT_TTL
    ): SyncValue<T> = createSyncValue(id, json.serializersModule.serializer(), defaultValue, ttl)

    /**
     * Creates a new [SyncValue] instance identified by [id].
     *
     * Must be called before [freeze].
     *
     * @param serializer Serializer used for the value.
     * @param defaultValue Initial/default value used by the implementation.
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
     * Creates a new [SyncMap] instance identified by [id].
     *
     * Must be called before [freeze].
     */
    inline fun <reified K : Any, reified V : Any> createSyncMap(
        id: String,
        ttl: Duration = SyncMap.DEFAULT_TTL
    ): SyncMap<K, V> = createSyncMap(id, json.serializersModule.serializer(), json.serializersModule.serializer(), ttl)

    /**
     * Creates a new [SyncMap] instance identified by [id].
     *
     * Must be called before [freeze].
     *
     * @param keySerializer Serializer used for keys.
     * @param valueSerializer Serializer used for values.
     */
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
     * Creates a [SimpleRedisCache] for the given [namespace] using a serializer derived from `V`.
     *
     * @param namespace Prefix placed before each Redis key.
     * @param ttl Time-to-live for cache entries.
     * @param keyToString Function that converts a key of type `K` to its string representation.
     */
    inline fun <K : Any, reified V : Any> createSimpleCache(
        namespace: String,
        ttl: Duration,
        noinline keyToString: (K) -> String = { it.toString() }
    ): SimpleRedisCache<K, V> = createSimpleCache(namespace, json.serializersModule.serializer(), ttl, keyToString)

    /**
     * Creates a [SimpleRedisCache] for the given [namespace] using the provided [serializer].
     *
     * @param namespace Prefix placed before each Redis key.
     * @param serializer Serializer used for cache values.
     * @param ttl Time-to-live for cache entries.
     * @param keyToString Function that converts a key of type `K` to its string representation.
     */
    fun <K : Any, V : Any> createSimpleCache(
        namespace: String,
        serializer: KSerializer<V>,
        ttl: Duration,
        keyToString: (K) -> String = { it.toString() }
    ): SimpleRedisCache<K, V> {
        return RedisComponentProvider.get().createSimpleCache(namespace, serializer, ttl, keyToString, this)
    }

    /**
     * Creates a [SimpleSetRedisCache] for the given [namespace] using a serializer derived from `T`.
     *
     * Must be called before [freeze].
     *
     * @param namespace Prefix placed before each Redis key.
     * @param ttl Time-to-live for cache entries.
     * @param idOf Function that extracts a stable identifier for elements.
     * @param indexes Optional index configuration for the set cache.
     */
    inline fun <reified T : Any> createSimpleSetRedisCache(
        namespace: String,
        ttl: Duration,
        noinline idOf: (T) -> String,
        indexes: RedisSetIndexes<T> = RedisSetIndexes.empty()
    ): SimpleSetRedisCache<T> =
        createSimpleSetRedisCache(namespace, json.serializersModule.serializer(), ttl, idOf, indexes)


    /**
     * Creates a [SimpleSetRedisCache] for the given [namespace] using the provided [serializer].
     *
     * Must be called before [freeze].
     *
     * @param namespace Prefix placed before each Redis key.
     * @param serializer Serializer used for elements.
     * @param ttl Time-to-live for cache entries.
     * @param idOf Function that extracts a stable identifier for elements.
     * @param indexes Optional index configuration for the set cache.
     */
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