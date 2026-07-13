# surf-redis

`surf-redis` is a Redis-based coordination and synchronization library designed for distributed
JVM applications.

It provides:
- a central Redis API abstraction
- event broadcasting
- request/response messaging
- replicated in-memory data structures (value, list, set, map)
- simple Redis-backed caches

The library is built on top of **Redisson**, **Reactor**, and **Kotlin coroutines**.

---

## Concepts

### RedisApi

`RedisApi` is the central entry point.  
It owns the Redis clients and manages the lifecycle of all Redis-backed components.

A typical application creates **exactly one** `RedisApi` instance and shares it across the system.

Lifecycle:
1. Create the API
2. Register listeners / handlers / sync structures
3. Freeze the API
4. Connect to Redis
5. Disconnect on shutdown

```kotlin
val redisApi = RedisApi.create()

redisApi.subscribeToEvents(SomeListener())
redisApi.registerRequestHandler(SomeRequestHandler())

redisApi.freezeAndConnect()
```

---

## Service Pattern (recommended)

In most applications, `RedisApi` is wrapped inside a service that owns its lifecycle and exposes
a global access point.

```kotlin
abstract class RedisService {

    val redisApi = RedisApi.create()

    fun connect() {
        register()
        redisApi.freezeAndConnect()
    }

    @MustBeInvokedByOverriders
    @ApiStatus.OverrideOnly
    protected open fun register() {
        // register listeners and request handlers
    }

    fun disconnect() {
        redisApi.disconnect()
    }

    companion object {
        val instance = requiredService<RedisService>()
        fun get() = instance

        fun publish(event: RedisEvent) =
            instance.redisApi.publishEvent(event)

        fun namespaced(suffix: String) =
            "example-namespace:$suffix"
    }
}

val redisApi get() = RedisService.get().redisApi

@AutoService(RedisService::class)
class PaperRedisService : RedisService() {
    override fun register() {
        super.register()
        // redisApi.subscribeToEvents(PaperListener())
    }
}
```

Sync structures are typically created **where they are used**, not inside the Redis service:

```kotlin
object SomeOtherService {
    private val counter =
        redisApi.createSyncValue<Int>(
            RedisService.namespaced("counter"),
            defaultValue = 0
        )
}
```

---

## Events

### RedisEvent

Events are broadcasted via Redis Pub/Sub.

```kotlin
@Serializable
class PlayerJoinedEvent(val playerId: UUID) : RedisEvent()
```

Publishing an event:

```kotlin
RedisService.publish(PlayerJoinedEvent(playerId))
```

### Event handlers

Handlers are discovered via `@OnRedisEvent`.

```kotlin
class PlayerListener {

    @OnRedisEvent
    fun onJoin(event: PlayerJoinedEvent) {
        if (event.originatesFromThisClient()) return
        println("Player joined on another node: ${event.playerId}")
    }
}
```

Handlers can also be `suspend` functions — they run directly in the listener coroutine:

```kotlin
class PlayerListener {

    @OnRedisEvent
    suspend fun onJoin(event: PlayerJoinedEvent) {
        if (event.originatesFromThisClient()) return
        val profile = fetchProfileSuspending(event.playerId)
        println("Profile: $profile")
    }
}
```

* Handlers are invoked inside a coroutine on **`Dispatchers.Default`**
* Both regular and `suspend` functions are supported
* **Do not perform blocking work directly** — switch dispatchers for blocking I/O:

```kotlin
@OnRedisEvent
suspend fun onJoin(event: PlayerJoinedEvent) {
    withContext(Dispatchers.IO) {
        loadFromDatabase(event.playerId)
    }
}
```

### Event wire formats and custom codecs

Existing events continue to use the unchanged JSON envelope on `surf-redis:events`. No migration is
required for existing users, and external panels can keep consuming that channel as JSON. An event
uses the binary path only when a `RedisEventCodec` is registered for its concrete class.

Custom-coded events are published exclusively on `surf-redis:events:binary`. Its compact envelope
contains a protocol version, stable event ID, codec version, timestamp, origin ID, and codec payload.
Binary payloads never appear on the JSON channel, so JSON-only consumers remain safe in mixed
deployments.

Old and new clients remain interoperable for every JSON event combination (old publisher/new
subscriber and new publisher/old subscriber included). Old clients do not subscribe to the binary
channel, so custom events are delivered only among updated clients that register a compatible
codec. Default JSON synchronized structures keep their existing Redis keys and representation;
custom-coded structures require updated clients with matching codec identities.

The simplest convention is a codec implemented by the event companion object. It is discovered
once when a listener registers the event type and then cached:

```kotlin
class PlayerLevelChanged(
    val playerId: UUID,
    val level: Int
) : RedisEvent() {
    companion object : RedisEventCodec<PlayerLevelChanged> {
        override val codecId = "example:player-level"
        override val version = 1
        override val eventId = "example:player-level-changed"

        override fun encode(buffer: ByteBuf, value: PlayerLevelChanged) {
            buffer.writeUuid(value.playerId)
            buffer.writeVarInt(value.level)
        }

        override fun decode(buffer: ByteBuf) = PlayerLevelChanged(
            playerId = buffer.readUuid(),
            level = buffer.readVarInt()
        )
    }
}
```

`eventId` defaults to the event's fully qualified class name for a companion codec. Override it if
the class may be renamed. For a publish-only type, call
`redisApi.registerEventType<PlayerLevelChanged>()` before `freeze()` so discovery and validation
happen during startup. Listener registration performs that step automatically.

Codecs can also be registered explicitly before `freeze()`:

```kotlin
redisApi.registerEventCodec(PlayerLevelChanged::class.java, playerLevelCodec)
// or: redisApi.registerEventCodec<PlayerLevelChanged>(playerLevelCodec)
```

Explicit registration takes precedence over an automatically discovered codec. Duplicate explicit
registrations, invalid IDs or versions, and event-ID collisions fail immediately. IDs must be stable
across processes; registration-order IDs are not supported.

If a receiver lacks an event ID or matching codec version, it logs that combination once and ignores
only those packets. Other JSON and binary events continue normally. Publishing before `connect()`
and registering after `freeze()` are rejected.

Codec instances may be invoked concurrently and must be thread-safe. They read and write a
caller-owned `ByteBuf` and must never retain, release, or store it. Event timestamp and origin
metadata are carried by the envelope and should not be duplicated by the codec.

---

## Request / Response

Requests are **broadcasted**.
Multiple servers may respond.
The **first response that arrives wins**.

### Defining requests and responses

```kotlin
@Serializable
class GetPlayerRequest(val playerId: UUID) : RedisRequest()

@Serializable
class PlayerResponse(val name: String) : RedisResponse()
```

### Handling requests

```kotlin
class PlayerRequestHandler {

    @HandleRedisRequest
    fun handle(ctx: RequestContext<GetPlayerRequest>) {
        if (ctx.originatesFromThisClient()) return

        val player = loadPlayer(ctx.request.playerId)
        ctx.respond(PlayerResponse(player.name))
    }
}
```

Handlers can also be `suspend` functions — [RequestContext.respond] can be called at any suspension point:

```kotlin
class PlayerRequestHandler {

    @HandleRedisRequest
    suspend fun handle(ctx: RequestContext<GetPlayerRequest>) {
        if (ctx.originatesFromThisClient()) return

        val player = fetchPlayerSuspending(ctx.request.playerId)
        ctx.respond(PlayerResponse(player.name))
    }
}
```

* Handlers are invoked inside a coroutine on **`Dispatchers.Default`**
* Both regular and `suspend` functions are supported
* **Do not perform blocking work directly** — switch to `Dispatchers.IO` for blocking calls:

```kotlin
@HandleRedisRequest
suspend fun handle(ctx: RequestContext<GetPlayerRequest>) {
    if (ctx.originatesFromThisClient()) return
    val player = withContext(Dispatchers.IO) { loadFromDatabaseBlocking(ctx.request.playerId) }
    ctx.respond(PlayerResponse(player.name))
}
```

Each handler may respond **at most once**.

### Sending requests

```kotlin
val response: PlayerResponse =
    redisApi.sendRequest(GetPlayerRequest(playerId))
```

If no response is received within the timeout, a `RequestTimeoutException` is thrown.

---

## Sync Structures

Sync structures maintain local in-memory state and synchronize it via Redis.
They are **eventually consistent**.

All sync structures:

* are created via `RedisApi`
* expose a local view
* propagate mutations asynchronously
* provide listener support

### SyncValue

Replicated single value.

```kotlin
val onlineCount =
    redisApi.createSyncValue(
        RedisService.namespaced("online-count"),
        defaultValue = 0
    )

onlineCount.set(onlineCount.get() + 1)
```

Property delegate support:

```kotlin
var count by onlineCount.asProperty()
count++
```

### SyncList

Replicated ordered list.

```kotlin
val players =
    redisApi.createSyncList<String>(
        RedisService.namespaced("players")
    )

players += "Alice"
players.remove("Bob")
```

### SyncSet

Replicated set.

```kotlin
val onlinePlayers =
    redisApi.createSyncSet<String>(
        RedisService.namespaced("online-players")
    )

onlinePlayers += "Alice"
onlinePlayers -= "Bob"
```

### SyncMap

Replicated key-value map.

```kotlin
val playerScores =
    redisApi.createSyncMap<UUID, Int>(
        RedisService.namespaced("scores")
    )

playerScores[playerId] = 42
```

### Custom codecs for synchronized structures

Every synchronized structure has an overload accepting a `RedisCodec`; maps accept independent key
and value codecs. The codec is used for snapshots, Redis storage, stream changes, local replication,
resynchronization, defaults, and reloads. Values go directly through the codec into a delimiter-safe
binary representation and are never converted through JSON.

```kotlin
data class PlayerState(val name: String, val score: Int)

object PlayerStateCodec : RedisCodec<PlayerState> {
    override val codecId = "example:player-state"
    override val version = 1

    override fun encode(buffer: ByteBuf, value: PlayerState) {
        buffer.writeString(value.name, maxLength = 64)
        buffer.writeVarInt(value.score)
    }

    override fun decode(buffer: ByteBuf) = PlayerState(
        name = buffer.readString(maxLength = 64),
        score = buffer.readVarInt()
    )
}

object UUIDBinaryCodec : RedisCodec<UUID> {
    override val codecId = "example:uuid-128"
    override fun encode(buffer: ByteBuf, value: UUID) = buffer.writeUuid(value)
    override fun decode(buffer: ByteBuf): UUID = buffer.readUuid()
}

val states = redisApi.createSyncMap(
    id = RedisService.namespaced("player-states"),
    keyCodec = UUIDBinaryCodec,
    valueCodec = PlayerStateCodec
)

val queue = redisApi.createSyncList(
    id = RedisService.namespaced("player-queue"),
    codec = UUIDBinaryCodec
)
```

The same `codec = ...` overload is available for `SyncSet` and `SyncValue` (with
`defaultValue = ...`). Create all structures and register all event codecs before `freeze()`.

`codecId` and `version` identify a structure's wire format. Clients using the same structure ID must
use matching identities. Surf Redis stores short-lived codec metadata beside custom-coded structures
and fails connection on a detected mismatch. It also refuses to attach a custom codec to existing
data with no codec metadata, because that data may use the legacy JSON representation.

Codec failures identify both codec and structure context. Payload and collection sizes have safety
limits to prevent uncontrolled allocation from malformed data. Structure codecs follow the same
thread-safety and caller-owned buffer rules as event codecs.

> [!IMPORTANT]
> Switching an existing synchronized structure from JSON to a custom codec is a data migration, not
> an in-place configuration change. Use a new structure ID or deliberately remove/migrate the old
> Redis keys while every client is stopped.

---

## Listeners

All sync structures support listeners:

```kotlin
playerScores.addListener { change ->
    when (change) {
        is SyncMapChange.Put -> println("Score updated: ${change.key}")
        is SyncMapChange.Removed -> println("Score removed: ${change.key}")
        is SyncMapChange.Cleared -> println("Scores cleared")
    }
}
```

Listener invocation thread depends on whether the change is local or remote.

---

## Caches

### SimpleRedisCache

Key-value cache with TTL.

```kotlin
val cache =
    redisApi.createSimpleCache<String, Player>(
        namespace = "players",
        ttl = 30.seconds
    )

cache.put("alice", player)
val cached = cache.get("alice")
```

### SimpleSetRedisCache

Set-based cache with optional indexes.

```kotlin
val cache =
    redisApi.createSimpleSetRedisCache(
        namespace = "players",
        ttl = 30.seconds,
        idOf = { it.id.toString() }
    )
```

---

## Internal APIs

Some APIs are annotated with `@InternalRedisAPI`.

These APIs:

* are **not stable**
* may change or be removed without notice
* must not be used by consumers

---

## Handler Dispatch (Internals)

Event handlers (`@OnRedisEvent`) and request handlers (`@HandleRedisRequest`) are dispatched
through **JVM hidden classes** generated at registration time.

Each handler method is resolved into a `MethodHandle`, which is then embedded as a
`static final` constant in a hidden class implementing `RedisEventInvoker` or
`RedisRequestHandlerInvoker`. This allows the JIT compiler to constant-fold and inline the
entire dispatch path — significantly outperforming raw `MethodHandle.invoke()` polymorphic calls.

Hidden classes are:

* **Not discoverable** by name — no classloader pollution
* **Garbage-collectible** when no longer referenced
* **JIT-friendly** — the dispatch target is a compile-time constant

This approach combines the flexibility of reflection-based handler discovery with
near-direct-call performance at runtime.

> [!NOTE]
> This is an implementation detail. The public API for registering handlers
> (`@OnRedisEvent`, `@HandleRedisRequest`) remains unchanged.

### Performance benchmarks

The codec benchmarks use JMH so warmup, JVM forks, dead-code elimination, and measurement timing
are handled independently from correctness tests. They compare the complete legacy JSON event
wire format with the custom binary event packet for 0, 32, 256, 1,024, 4,096, 16,384, and 65,536
bytes of application payload.

```bash
# Full run: 3 warmup iterations, 5 measured iterations, and 2 JVM forks
./gradlew :surf-redis-core:codecBenchmark

# Short harness/fixture validation using every operation and payload size
./gradlew :surf-redis-core:jmh -PbenchmarkSmoke

# Exact encoded packet sizes without benchmark timing noise
./gradlew :surf-redis-core:eventWireSizeReport
```

Separate JMH cases cover JSON and binary encode, decode, and full round trips. The GC profiler also
reports allocation rate and bytes allocated per operation. Machine-readable results are written to
`surf-redis-core/build/results/jmh/results.json`. Compare results only from the same machine, JDK,
JVM flags, and system load.

Selected codec-registry concurrency scenarios use JetBrains Lincheck model checking. They cover
registration, event-ID collisions, freezing, and frozen lookups and run automatically as part of
`check`; they can also be run directly:

```bash
./gradlew :surf-redis-core:lincheckTest
```

---

## Guarantees & Non-Guarantees

Guaranteed:

* Local operations are non-blocking
* Eventual convergence across nodes
* At-most-once response per request handler
* First response wins for requests

Not guaranteed:

* Strong consistency
* Total ordering across nodes
* Delivery in the presence of network partitions
* Exactly-once semantics

---

## Common Pitfalls

### 1. Creating sync structures after `freeze()`

All sync structures (`SyncValue`, `SyncList`, `SyncSet`, `SyncMap`) **must be created before**
`RedisApi.freeze()` is called.

This can be subtle in larger applications, because sync structures are often created in
*other services* that depend on Redis, not inside the Redis service itself.

**Problematic setup**

```kotlin
class SomeService {
    private val counter =
        redisApi.createSyncValue("counter", 0) // <-- may run too late
}
```

If `SomeService` is initialized **after** `RedisApi.freeze()` has already been called,
sync structure creation will fail or behave incorrectly.

**Correct approach**

You must ensure that **all services that create sync structures are initialized
before the Redis API is frozen**.

A common pattern is:

* RedisService owns the `RedisApi`
* All dependent services are initialized *before* `connect()` is called

```kotlin
class RedisService {

    val redisApi = RedisApi.create()

    fun connect() {
        // Ensure all services that create sync structures are initialized here
        SomeService.init()
        AnotherService.init()

        redisApi.freezeAndConnect()
    }
}
```

Alternatively, ensure that service initialization order guarantees that
all sync structures are created eagerly during startup.

**Rule of thumb:**

> [!TIP]
> If a service creates sync structures, it must be initialized before `RedisApi.freeze()`.

---

### 2. Treating sync structures as strongly consistent

Sync structures are **eventually consistent**.

Do **not** assume:

* immediate visibility on other nodes
* global ordering guarantees
* exactly-once delivery

They are designed for coordination and shared state, not transactional consistency.

---

### 3. Doing blocking work in event or request handlers

Event handlers (`@OnRedisEvent`) and request handlers (`@HandleRedisRequest`) are invoked
inside a coroutine launched on **`Dispatchers.Default`**.

Both regular and `suspend` handler methods are supported. However, even though handlers run
in a coroutine, **do not perform blocking work directly**:

* blocking database drivers
* file I/O
* blocking network calls
* long CPU-intensive computations

Blocking a `Dispatchers.Default` thread stalls the shared thread pool and degrades the entire application.
Always switch the dispatcher for blocking operations.

`RequestContext` implements `CoroutineScope` — you can `launch` additional coroutines from it.
Pick the right dispatcher for the work being done:

```kotlin
// CPU-intensive work — Default dispatcher is already correct, no switch needed
@HandleRedisRequest
suspend fun handle(ctx: RequestContext<MyRequest>) {
    val result = computeSomething()
    ctx.respond(MyResponse(result))
}

// Blocking I/O (blocking DB drivers, file access, blocking network calls) — switch to Dispatchers.IO
@HandleRedisRequest
suspend fun handle(ctx: RequestContext<MyRequest>) {
    val result = withContext(Dispatchers.IO) {
        loadFromDatabaseBlocking()
    }
    ctx.respond(MyResponse(result))
}

// Non-blocking / suspending I/O (e.g. R2DBC, async clients) — no dispatcher switch needed
@HandleRedisRequest
suspend fun handle(ctx: RequestContext<MyRequest>) {
    val result = loadFromDatabaseSuspending()
    ctx.respond(MyResponse(result))
}

// From a non-suspend handler — launch a coroutine manually
@HandleRedisRequest
fun handle(ctx: RequestContext<MyRequest>) {
    ctx.launch(Dispatchers.IO) {
        val result = loadFromDatabaseBlocking()
        ctx.respond(MyResponse(result))
    }
}
```

The same applies to event handlers:

```kotlin
// Suspend event handler with blocking I/O
@OnRedisEvent
suspend fun onJoin(event: PlayerJoinedEvent) {
    if (event.originatesFromThisClient()) return
    withContext(Dispatchers.IO) {
        loadFromDatabase(event.playerId)
    }
}
```

---

### 4. Responding multiple times to a request

Each `RequestContext` allows **exactly one** response.

Calling `respond()` more than once will throw an exception.
This includes responding both synchronously *and* asynchronously by mistake.

---

### 5. Ignoring `originatesFromThisClient()`

Requests and events are broadcasted.

If a handler should not process self-originated messages, it must explicitly check:

```kotlin
if (event.originatesFromThisClient()) return
```

This is especially important to avoid feedback loops.

---

### 6. Assuming sync structures survive full cluster shutdown

Sync structures rely on Redis keys with TTL.

If **all nodes go offline**, the remote state may expire.
The next node will start from the default/snapshot behavior defined by the structure.

Design your application logic accordingly.
