# surf-redis

`surf-redis` is a Redis-based coordination and synchronization library designed for distributed
JVM applications.

It provides:
- a central Redis API abstraction
- replicated in-memory data structures (value, list, set, map)
- simple Redis-backed caches

The library is built on top of **Redisson**, **Reactor**, and **Kotlin coroutines**.

Events and request/response messaging moved to [surf-nats](../nats/README.md#migrating-from-surf-redis-and-surf-rabbitmq).

---

## Configuration

Connection settings come from `config.yml` in the data directory and can be overridden by
environment variables. The same four keys listed below are the ones available in `config.yml`.

### Environment variables

Each variable overrides the matching `config.yml` key. Precedence is
**environment variable → `config.yml` → built-in default**.

| Variable                 | Type                  | `config.yml` key | Default                             |
|--------------------------|-----------------------|------------------|-------------------------------------|
| `SURF_REDIS_HOST`        | string                | `host`           | `localhost`                         |
| `SURF_REDIS_PORT`        | int, `0`–`65535`      | `port`           | `6379`                              |
| `SURF_REDIS_PASSWORD`    | string (secret)       | `password`       | none (no authentication)            |
| `SURF_REDIS_CLIENT_NAME` | string                | `clientName`     | `surf-redis-client-<random UUID>`   |

```bash
export SURF_REDIS_HOST=redis.internal
export SURF_REDIS_PORT=6380
export SURF_REDIS_PASSWORD='super-secret'
export SURF_REDIS_CLIENT_NAME=lobby-01
```

`SURF_REDIS_CLIENT_NAME` is used as-is as the Redis client name (`CLIENT SETNAME`). All plugins in the
process that connect with the same host, port and credentials share one Redisson connection pool, so
the name identifies the server process rather than an individual plugin.

### Resolution behavior

**Set-but-empty is not the same as unset.** A variable only falls back to `config.yml` when it is
*missing* from the environment. A variable that is present but empty wins the override with an empty
value — `SURF_REDIS_HOST=""` produces an empty host, not `localhost`, and an empty
`SURF_REDIS_CLIENT_NAME` produces an empty client name. `SURF_REDIS_PASSWORD` is the
only exception: an empty password is treated as no authentication. To fall back to `config.yml`,
**unset the variable** instead of blanking it.

**Values are read once.** Only the process environment (`System.getenv`) is consulted — no `.env`
file and no JVM system properties. `config.yml` and the environment are snapshotted together on
first access, so changing the environment afterwards has no effect.

**Invalid values fail on first use.** A non-numeric or out-of-range `SURF_REDIS_PORT` throws when the
configuration is first accessed — during connection setup, not at plugin load. The error names the
offending variable; the raw value of `SURF_REDIS_PASSWORD` is redacted from failure messages.

---

## Concepts

### RedisApi

`RedisApi` is the central entry point.  
It manages the lifecycle of all Redis-backed components and holds a share of the underlying Redisson
client.

A typical application creates **exactly one** `RedisApi` instance and shares it across the system.

### Connection sharing

Every `RedisApi` in the same process whose connection settings are equivalent — same host, port,
TLS/Unix-socket transport and credentials — shares **one** physical Redisson client and connection
pool. Ten plugins on one server therefore use a single pool instead of ten. Connections that differ in
any of these settings stay isolated.

Only the connection is shared. Each `RedisApi` keeps its own sync structures, caches and coroutine
scopes. Connecting or disconnecting one instance does not affect another:

* the first instance to connect creates the shared client, later ones join it
* `disconnect()` releases the instance's share; the client is shut down once the last instance using
  it has disconnected
* a connection failure leaves nothing behind, and an instance whose components fail to initialize
  releases its share again
* clients that are still held when the surf-redis platform plugin is disabled are shut down and their
  owners logged as a warning

Lifecycle:
1. Create the API
2. Create sync structures and caches
3. Freeze the API
4. Connect to Redis
5. Disconnect on shutdown

```kotlin
val redisApi = RedisApi.create()

val counter = redisApi.createSyncValue<Int>("example-namespace:counter", defaultValue = 0)

redisApi.freezeAndConnect()
```

---

## Service Pattern (recommended)

In most applications, `RedisApi` is wrapped inside a service that owns its lifecycle and exposes
a global access point.

```kotlin
class RedisService {

    val redisApi = RedisApi.create()

    fun connect() {
        redisApi.freezeAndConnect()
    }

    fun disconnect() {
        redisApi.disconnect()
    }

    companion object {
        val instance = requiredService<RedisService>()
        fun get() = instance

        fun namespaced(suffix: String) =
            "example-namespace:$suffix"
    }
}

val redisApi get() = RedisService.get().redisApi
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
    override val codecId = "example:player-state" // optional
    override val version = 1 // optional

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
`defaultValue = ...`). Create all structures before `freeze()`.

`codecId` and `version` identify a structure's wire format. Clients using the same structure ID must
use matching identities. Surf Redis stores short-lived codec metadata beside custom-coded structures
and fails connection on a detected mismatch. It also refuses to attach a custom codec to existing
data with no codec metadata, because that data may use the JSON representation.

Codec failures identify both codec and structure context. Payload and collection sizes have safety
limits to prevent uncontrolled allocation from malformed data. Codec instances may be invoked
concurrently and must be thread-safe. They read and write a caller-owned `ByteBuf` and must never
retain, release, or store it.

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

## Concurrency tests

The connection-sharing registry is checked with JetBrains Lincheck model checking. These tests run
as part of `check`; they can also be run directly:

```bash
./gradlew :surf-redis-core:lincheckTest
```

---

## Guarantees & Non-Guarantees

Guaranteed:

* Local operations are non-blocking
* Eventual convergence across nodes

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

### 3. Assuming sync structures survive full cluster shutdown

Sync structures rely on Redis keys with TTL.

If **all nodes go offline**, the remote state may expire.
The next node will start from the default/snapshot behavior defined by the structure.

Design your application logic accordingly.
