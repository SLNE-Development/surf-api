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
````

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

* Handlers are invoked **synchronously** on a Redisson/Reactor thread
* Long-running or suspending work must be delegated manually

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
````

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
**synchronously** on a Redisson/Reactor thread.

Do not block:

* database calls
* file I/O
* long computations

Delegate explicitly:

```kotlin
@HandleRedisRequest
fun handle(ctx: RequestContext<MyRequest>) {
    coroutineScope.launch {
        val result = doBlockingWork()
        ctx.respond(MyResponse(result))
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