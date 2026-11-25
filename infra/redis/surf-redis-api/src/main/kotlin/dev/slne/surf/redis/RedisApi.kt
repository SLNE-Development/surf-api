package dev.slne.surf.redis

import com.github.benmanes.caffeine.cache.Caffeine
import dev.slne.surf.api.common.util.getCallerClass
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.serialization.SurfSerializerModule
import dev.slne.surf.api.serialization.java.uuid.JavaUUIDStringSerializer
import dev.slne.surf.redis.cache.RedisSetIndexes
import dev.slne.surf.redis.cache.SimpleRedisCache
import dev.slne.surf.redis.cache.SimpleSetRedisCache
import dev.slne.surf.redis.codec.RedisCodec
import dev.slne.surf.redis.credentials.RedisCredentialsProvider
import dev.slne.surf.redis.internal.RedissonConfigDetails
import dev.slne.surf.redis.internal.RedissonConnectionKey
import dev.slne.surf.redis.internal.SharedRedissonClient
import dev.slne.surf.redis.internal.SharedRedissonClientRegistry
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.list.SyncList
import dev.slne.surf.redis.sync.map.SyncMap
import dev.slne.surf.redis.sync.set.SyncSet
import dev.slne.surf.redis.sync.value.SyncValue
import dev.slne.surf.redis.util.Initializable
import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.serializer
import org.jetbrains.annotations.Blocking
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.redisson.api.redisnode.RedisNodes
import org.redisson.codec.BaseEventCodec
import org.redisson.config.Config
import org.redisson.config.ConfigSupport
import org.redisson.misc.RedisURI
import reactor.core.Disposable
import reactor.core.publisher.Mono
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * Central entry point for surf-redis.
 *
 * `RedisApi` wires up the higher-level surf-redis features on top of a Redisson client:
 * - replicated in-memory data structures ([SyncList], [SyncSet], [SyncMap], [SyncValue])
 * - simple cache helpers ([SimpleRedisCache], [SimpleSetRedisCache])
 *
 * Messaging (events and request/response) is provided by surf-nats.
 *
 * Instances are created via [create] and manage their own lifecycle.
 *
 * ## Connection sharing
 * Every `RedisApi` in the process whose connection configuration is equivalent (same endpoint,
 * transport and credentials, see [RedissonConnectionKey]) shares one physical Redisson client and
 * connection pool. The client is created by the first instance that connects and shut down when the
 * last instance using it disconnects. Sharing is limited to the connection: sync structures, caches
 * and coroutine scopes belong to the individual instance, and
 * connecting or disconnecting one instance never affects another.
 *
 * ## Lifecycle
 * This API follows a two-phase setup:
 * 1. Create an instance via [create]
 * 2. Create sync structures and caches
 * 3. Call [freeze] to lock configuration
 * 4. Call [connect] to acquire the Redis connection and start all registered features
 *
 * Use [freezeAndConnect] as a convenience for steps 3 and 4.
 *
 * Call [disconnect] to dispose the instance's resources and release its share of the connection.
 *
 * ## Usage in a surf-api plugin
 * Every plugin context holds a managed `RedisApi`. A bean injects it and creates its sync
 * structures and caches while the context refreshes: in a constructor, a property initializer or a
 * `@Bean` method. surf-api freezes and connects the instance right after the context has refreshed,
 * and disconnects it when the context closes, so a plugin never calls [freeze], [connect] or
 * [disconnect] on it.
 *
 * ```
 * @Service
 * class OnlineService(redis: RedisApi) {
 *     private val online = redis.createSyncSet<UUID>("online-players")
 * }
 * ```
 *
 * A structure created after the instance is frozen fails with [IllegalStateException].
 *
 * ## Availability
 * surf-api provides Redis on Paper, Velocity and Minestom only; there is no standalone module.
 */
@Suppress("unused")
class RedisApi private constructor(
    private val config: Config,
    private val connectionKey: RedissonConnectionKey,
    private val pluginName: String,
    /**
     * JSON instance used internally for (de-)serialization.
     */
    val json: Json,
) {
    private val parsedConfig = ConfigSupport.getConfig(config)

    private val ownerName = "${parsedConfig.clientName}-$pluginName"

    @Volatile
    private var lease: SharedRedissonClientRegistry.Lease? = null

    private val sharedClient: SharedRedissonClient
        get() = lease?.client
            ?: error("RedisApi '$ownerName' is not connected; call connect() first")

    /**
     * Underlying Redisson client, shared with every [RedisApi] using an equivalent connection.
     *
     * Available once [connect] has acquired the connection; accessing it earlier fails. After
     * [disconnect] it still returns the last client used, which may be shut down or owned by other
     * instances, and must not be used anymore. Never shut it down directly.
     */
    val redisson: RedissonClient
        get() = sharedClient.redisson

    /**
     * Reactive view of [redisson], shared the same way.
     *
     * Intended for reactive command usage in internal components.
     */
    val redissonReactive: RedissonReactiveClient
        get() = sharedClient.redissonReactive

    /**
     * Redis OS type as reported by `INFO server` (used for Redisson codec behavior).
     *
     * Detected once per shared connection and available after [connect]. `null` before connecting
     * or when no special handling is required.
     */
    val redisOsType: BaseEventCodec.OSType?
        get() = lease?.client?.redisOsType

    /**
     * Identifier of the current client/node as provided by the component provider.
     */
    val clientId get() = RedisComponentProvider.instance.clientId

    private val syncStructureScope = CoroutineScope(
        Dispatchers.Default
            + SupervisorJob()
            + CoroutineName("surf-redis-sync-structures-$ownerName")
            + CoroutineExceptionHandler { context, throwable ->
            log.atSevere()
                .withCause(throwable)
                .log(
                    "Uncaught exception in Redis sync structure coroutine (context: ${
                        context.toString().replace("{", "[").replace("}", "]")
                    })",
                )
        },
    )

    private val initializables = Caffeine.newBuilder().weakKeys().build<Initializable, Unit>()
    private val disposables = Caffeine.newBuilder().weakKeys().build<Disposable, Unit>()

    @Volatile
    private var frozen = false

    @Volatile
    private var connectionState = ConnectionState.NEW

    private val lifecycleLock = ReentrantLock()

    private enum class ConnectionState {
        NEW,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }

    companion object {
        private val log = logger()

        /**
         * Creates a [RedisApi] instance using the given [redisURI].
         *
         * This is the most explicit factory method. The [pluginName] is used for
         * identification and logging purposes and is typically derived automatically
         * via the caller when not provided explicitly.
         *
         * @param redisURI Redis connection URI.
         * @param pluginName Logical name of the calling plugin or component.
         * @param serializerModule Additional serializers to be included in the internal [Json]
         * instance.
         */
        fun create(
            redisURI: RedisURI,
            pluginName: String,
            serializerModule: SerializersModule,
        ): RedisApi {
            val details = RedissonConfigDetails(
                redisURI = redisURI,
                serializerModule = serializerModule,
                pluginName = pluginName,
            )

            val components = RedisComponentProvider.instance

            return RedisApi(
                config = components.createRedissonConfig(details),
                connectionKey = components.createRedissonConnectionKey(details),
                pluginName = pluginName,
                json = createJson(serializerModule),
            )
        }

        /**
         * Creates a [RedisApi] instance using the given [redisURI].
         *
         * The plugin name is resolved automatically from the calling class.
         *
         * @param redisURI Redis connection URI.
         * @param serializerModule Additional serializers to be included in the internal [Json]
         * instance.
         */
        fun create(
            redisURI: RedisURI,
            serializerModule: SerializersModule = EmptySerializersModule(),
        ): RedisApi = create(redisURI, getCallingPluginName(), serializerModule)

        /**
         * Creates a [RedisApi] instance using credentials provided by
         * [RedisCredentialsProvider].
         *
         * The plugin name is resolved automatically from the calling class.
         *
         * @param serializerModule Additional serializers to be included in the internal [Json]
         * instance.
         */
        fun create(
            serializerModule: SerializersModule = EmptySerializersModule(),
        ): RedisApi {
            return create(
                RedisCredentialsProvider.instance.redisURI(),
                getCallingPluginName(),
                serializerModule,
            )
        }

        /**
         * Creates a [RedisApi] instance using the default Redis credentials.
         *
         * @param pluginName Logical name of the calling plugin or component.
         */
        fun create(pluginName: String): RedisApi {
            return create(
                RedisCredentialsProvider.instance.redisURI(),
                pluginName,
                EmptySerializersModule(),
            )
        }

        /**
         * Creates a [RedisApi] instance using the default Redis credentials.
         *
         * @param pluginName Logical name of the calling plugin or component.
         * @param serializerModule Additional serializers to be included in the internal [Json]
         * instance.
         */
        fun create(pluginName: String, serializerModule: SerializersModule): RedisApi {
            return create(
                RedisCredentialsProvider.instance.redisURI(),
                pluginName,
                serializerModule,
            )
        }

        private fun createJson(serializerModule: SerializersModule): Json {
            return Json {
                ignoreUnknownKeys = true
                namingStrategy = JsonNamingStrategy.SnakeCase
                encodeDefaults = true
                serializersModule = SerializersModule {
                    include(
                        SurfSerializerModule.all.overwriteWith(
                            SerializersModule {
                                // UUID as string rather than byte array
                                contextual(JavaUUIDStringSerializer)
                            },
                        ),
                    )

                    include(serializerModule)
                }
            }
        }

        private fun getCallingPluginName(): String {
            val caller = getCallerClass(1) ?: return "Unknown"

            return RedisComponentProvider.instance.tryExtractPluginNameFromClass(caller)
        }
    }

    /**
     * Acquires the Redis connection and starts all registered features.
     *
     * This method is blocking and must only be called once. A disconnected instance cannot be
     * reconnected because its managed structures and coroutine scopes have been disposed; create a
     * new [RedisApi] for a later connection lifecycle.
     * The API must be [freeze]d before connecting to ensure registrations are complete.
     *
     * During connection:
     * - a lease on the shared Redisson client for this connection configuration is acquired; the
     *   client is created if no other [RedisApi] currently uses it
     * - [redisson], [redissonReactive] and [redisOsType] become available
     * - all previously created sync structures are initialized
     *
     * If the connection cannot be acquired, nothing is retained and the instance may retry. If a
     * component fails to initialize afterwards, the instance disposes its resources, releases the
     * connection and becomes terminally disconnected. Other instances sharing the connection are
     * not affected in either case.
     *
     * @throws IllegalArgumentException if the API is not frozen
     * @throws IllegalArgumentException if already connected or already disconnected
     */
    @Blocking
    fun connect(): RedisApi {
        return apply {
            lifecycleLock.withLock {
                require(isFrozen()) { "Redis client must be frozen before connecting" }
                require(connectionState != ConnectionState.CONNECTED) {
                    "Redis client already initialized"
                }
                require(connectionState == ConnectionState.NEW) {
                    "RedisApi cannot reconnect after disconnect; create and configure a new RedisApi instance"
                }

                log.atInfo()
                    .log("Connecting %s to Redis...", ownerName)

                val lease = SharedRedissonClientRegistry.instance.acquire(
                    connectionKey,
                    ownerName,
                    config,
                )
                this.lease = lease
                connectionState = ConnectionState.CONNECTED

                try {
                    initializeComponents()
                } catch (failure: Throwable) {
                    connectionState = ConnectionState.DISCONNECTING

                    try {
                        disposeManagedResources()
                    } catch (cleanupFailure: Throwable) {
                        failure.addSuppressed(cleanupFailure)
                    }

                    connectionState = ConnectionState.DISCONNECTED

                    try {
                        lease.release()
                    } catch (releaseFailure: Throwable) {
                        failure.addSuppressed(releaseFailure)
                    }

                    throw failure
                }
            }
        }
    }

    @Blocking
    private fun initializeComponents() {
        val initializables = this.initializables.asMap().keys

        if (initializables.isEmpty()) {
            log.atInfo()
                .log("No initializable Redis components registered; skipping initialization step.")

            return
        }

        Mono.`when`(
            initializables.map { initializable ->
                initialize(initializable)
            },
        ).doOnError { throwable ->
            log.atSevere()
                .withCause(throwable)
                .log(
                    "RedisApi.connect() failed because one or more components could not be initialized.",
                )
        }.block()
    }

    private fun initialize(initializable: Initializable): Mono<Void> {
        return initializable.init()
            .doOnError { throwable ->
                log.atSevere()
                    .withCause(throwable)
                    .log(
                        "Failed to initialize Redis component: %s",
                        initializable::class.qualifiedName ?: initializable.toString(),
                    )
            }
    }

    /**
     * Convenience method that calls [freeze] and then [connect].
     *
     * This method is blocking.
     */
    @Blocking
    fun freezeAndConnect(): RedisApi {
        return apply {
            freeze()
            connect()
        }
    }

    /**
     * Freezes this API instance and prevents further registrations.
     *
     * After freezing:
     * - creating sync structures via `createSync*` and caches via `createSimple*` is no longer
     *   allowed
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
     * Disposes all resources created by this API and releases its share of the Redis connection.
     *
     * This method is safe to call multiple times; if not connected it has no effect. Disconnect is
     * terminal for this instance. A disconnect that overlaps a running [connect] waits for it to
     * finish first.
     *
     * On disconnect:
     * - all sync structures are disposed
     * - the internal sync structure coroutine scope is cancelled
     * - reactive disposables are disposed
     * - the shared Redisson client is released; it is shut down only if no other [RedisApi] still
     *   uses it
     */
    @Blocking
    fun disconnect() {
        lifecycleLock.withLock {
            if (connectionState != ConnectionState.CONNECTED) return

            connectionState = ConnectionState.DISCONNECTING

            var failure: Throwable? = null

            try {
                disposeManagedResources()
            } catch (disposeFailure: Throwable) {
                failure = disposeFailure
            }

            connectionState = ConnectionState.DISCONNECTED

            try {
                lease?.release()
            } catch (releaseFailure: Throwable) {
                if (failure == null) failure = releaseFailure else failure.addSuppressed(
                    releaseFailure,
                )
            }

            failure?.let { throw it }
        }
    }

    private fun disposeManagedResources() {
        var cleanupFailure: Throwable? = null
        fun cleanup(action: () -> Unit) {
            try {
                action()
            } catch (failure: Throwable) {
                val current = cleanupFailure
                if (current == null) cleanupFailure = failure else current.addSuppressed(failure)
            }
        }

        val disposables = this.disposables.asMap().keys

        for (disposable in disposables) {
            cleanup(disposable::dispose)
        }

        disposables.clear()

        cleanup { syncStructureScope.cancel("RedisApi disconnected") }

        cleanupFailure?.let { throw it }
    }

    /**
     * Indicates whether this instance holds the Redis connection and is not disconnecting.
     *
     * This does not guarantee Redis availability; it only reflects the local lifecycle state of
     * this instance, independent of other instances sharing the same client.
     */
    fun isConnected(): Boolean = connectionState == ConnectionState.CONNECTED

    /**
     * Performs an active health check against Redis.
     *
     * Sends a `PING` and waits for the response.
     *
     * @return `true` if Redis responds successfully, `false` otherwise.
     */
    suspend fun isAlive(): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                redisson.getRedisNodes(RedisNodes.SINGLE).pingAll()
            }
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            false
        }
    }

    /**
     * Creates a new [SyncList] instance identified by [id].
     *
     * Must be called before [freeze].
     *
     * @param id Logical identifier used by the structure (typically part of the Redis key
     * namespace).
     * @param ttl Time-to-live used by the structure implementation.
     */
    inline fun <reified E : Any> createSyncList(
        id: String,
        ttl: Duration = SyncList.DEFAULT_TTL,
    ): SyncList<E> = createSyncList(id, json.serializersModule.serializer(), ttl)

    /**
     * Creates a new [SyncList] instance identified by [id].
     *
     * Must be called before [freeze].
     *
     * @param id Logical identifier used by the structure (typically part of the Redis key
     * namespace).
     * @param elementSerializer Serializer used for elements.
     * @param ttl Time-to-live used by the structure implementation.
     */
    fun <E : Any> createSyncList(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration = SyncList.DEFAULT_TTL,
    ): SyncList<E> {
        return createSyncStructure {
            RedisComponentProvider.instance.createSyncList(id, elementSerializer, ttl, this)
        }
    }

    /**
     * Creates a [SyncList] whose elements are encoded directly with [codec].
     *
     * Every client using the same [id] must use a codec with the same stable ID and version.
     * Incompatible configurations are rejected during [connect].
     */
    fun <E : Any> createSyncList(
        id: String,
        codec: RedisCodec<E>,
        ttl: Duration = SyncList.DEFAULT_TTL,
    ): SyncList<E> {
        return createSyncStructure {
            RedisComponentProvider.instance.createSyncList(id, codec, ttl, this)
        }
    }

    /**
     * Creates a new [SyncSet] instance identified by [id].
     *
     * Must be called before [freeze].
     */
    inline fun <reified E : Any> createSyncSet(
        id: String,
        ttl: Duration = SyncSet.DEFAULT_TTL,
    ): SyncSet<E> = createSyncSet(id, json.serializersModule.serializer(), ttl)

    /**
     * Creates a new [SyncSet] instance identified by [id].
     *
     * Must be called before [freeze].
     */
    fun <E : Any> createSyncSet(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration = SyncSet.DEFAULT_TTL,
    ): SyncSet<E> {
        return createSyncStructure {
            RedisComponentProvider.instance.createSyncSet(id, elementSerializer, ttl, this)
        }
    }

    /**
     * Creates a [SyncSet] whose elements are encoded directly with [codec].
     * Incompatible codec identities for the same [id] fail during [connect].
     */
    fun <E : Any> createSyncSet(
        id: String,
        codec: RedisCodec<E>,
        ttl: Duration = SyncSet.DEFAULT_TTL,
    ): SyncSet<E> {
        return createSyncStructure {
            RedisComponentProvider.instance.createSyncSet(id, codec, ttl, this)
        }
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
        ttl: Duration = SyncValue.DEFAULT_TTL,
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
        ttl: Duration = SyncValue.DEFAULT_TTL,
    ): SyncValue<T> {
        return createSyncStructure {
            RedisComponentProvider.instance.createSyncValue(id, serializer, defaultValue, ttl, this)
        }
    }

    /**
     * Creates a [SyncValue] encoded directly with [codec]. The codec is used for the initial
     * snapshot, updates, stream replication, resynchronization, and reconnect reloads.
     */
    fun <T : Any> createSyncValue(
        id: String,
        codec: RedisCodec<T>,
        defaultValue: T,
        ttl: Duration = SyncValue.DEFAULT_TTL,
    ): SyncValue<T> {
        return createSyncStructure {
            RedisComponentProvider.instance.createSyncValue(id, codec, defaultValue, ttl, this)
        }
    }

    /**
     * Creates a new [SyncMap] instance identified by [id].
     *
     * Must be called before [freeze].
     */
    inline fun <reified K : Any, reified V : Any> createSyncMap(
        id: String,
        ttl: Duration = SyncMap.DEFAULT_TTL,
    ): SyncMap<K, V> {
        return createSyncMap(
            id,
            json.serializersModule.serializer(),
            json.serializersModule.serializer(),
            ttl,
        )
    }

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
        ttl: Duration = SyncMap.DEFAULT_TTL,
    ): SyncMap<K, V> {
        return createSyncStructure {
            RedisComponentProvider.instance.createSyncMap(
                id,
                keySerializer,
                valueSerializer,
                ttl,
                this,
            )
        }
    }

    /**
     * Creates a [SyncMap] with independently encoded keys and values.
     *
     * Both codecs are used throughout snapshots, Redis hashes, stream changes, resynchronization,
     * and reconnect reloads. Every client using the same [id] must use matching codec identities.
     */
    fun <K : Any, V : Any> createSyncMap(
        id: String,
        keyCodec: RedisCodec<K>,
        valueCodec: RedisCodec<V>,
        ttl: Duration = SyncMap.DEFAULT_TTL,
    ): SyncMap<K, V> {
        return createSyncStructure {
            RedisComponentProvider.instance.createSyncMap(id, keyCodec, valueCodec, ttl, this)
        }
    }

    /**
     * Creates a new instance of a synchronization structure using the provided creator function.
     * Ensures that the Redis client is not frozen before creating the synchronization structure.
     * The created structure is added to the internal list of sync structures managed by the Redis
     * API.
     *
     * @param creator A factory function responsible for creating a specific type of synchronization
     * structure.
     * @return The newly created synchronization structure of type [S].
     * @throws IllegalStateException if the Redis client is frozen when attempting to create the
     * structure.
     */
    private inline fun <S : SyncStructure<*>> createSyncStructure(creator: () -> S): S {
        check(!isFrozen()) { "Redis client must not be frozen to create sync structures" }

        val structure = creator()
        initializables.put(structure, Unit)
        disposables.put(structure, Unit)

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
        noinline keyToString: (K) -> String = { it.toString() },
    ): SimpleRedisCache<K, V> {
        return createSimpleCache(namespace, json.serializersModule.serializer(), ttl, keyToString)
    }

    /**
     * Creates a [SimpleRedisCache] for the given [namespace] using the provided [serializer].
     *
     * Must be called before [freeze].
     *
     * @param namespace Prefix placed before each Redis key.
     * @param serializer Serializer used for cache values.
     * @param ttl Time-to-live for cache entries.
     * @param keyToString Function that converts a key of type `K` to its string representation.
     * @throws IllegalStateException if the API is already frozen
     */
    fun <K : Any, V : Any> createSimpleCache(
        namespace: String,
        serializer: KSerializer<V>,
        ttl: Duration,
        keyToString: (K) -> String = { it.toString() },
    ): SimpleRedisCache<K, V> {
        check(!isFrozen()) { "Redis client must not be frozen to create simple caches" }

        val cache = RedisComponentProvider.instance.createSimpleCache(
            namespace,
            serializer,
            ttl,
            keyToString,
            this,
        )

        initializables.put(cache, Unit)
        disposables.put(cache, Unit)

        return cache
    }

    /**
     * Creates a [SimpleSetRedisCache] for the given [namespace] using a serializer derived from
     * `T`.
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
        indexes: RedisSetIndexes<T> = RedisSetIndexes.empty(),
    ): SimpleSetRedisCache<T> {
        return createSimpleSetRedisCache(
            namespace,
            json.serializersModule.serializer(),
            ttl,
            idOf,
            indexes,
        )
    }

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
     * @throws IllegalStateException if the API is already frozen
     */
    fun <T : Any> createSimpleSetRedisCache(
        namespace: String,
        serializer: KSerializer<T>,
        ttl: Duration,
        idOf: (T) -> String,
        indexes: RedisSetIndexes<T> = RedisSetIndexes.empty(),
    ): SimpleSetRedisCache<T> {
        check(!isFrozen()) { "Redis client must not be frozen to create simple caches" }

        val cache = RedisComponentProvider.instance.createSimpleSetRedisCache(
            namespace,
            serializer,
            ttl,
            idOf,
            indexes,
            this,
        )

        initializables.put(cache, Unit)
        disposables.put(cache, Unit)

        return cache
    }
}
