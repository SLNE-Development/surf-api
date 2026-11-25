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

Connection settings come from `redis/config.yml` in the data folder of surf-api. Environment variables
override them. `config.yml` accepts the four keys listed in the table below, spelled in kebab-case
(`client-name`, not `clientName`); a key in any other spelling is ignored.

A `config.yml` of the retired `surf-redis-paper`, `surf-redis-velocity` and `surf-redis-minestom` plugins (also as
`surf-api-redis-paper`, `surf-api-redis-velocity` and `surf-api-redis-minestom`) is copied there once, on the first
start without `redis/config.yml`.

### Environment variables

Each variable overrides the matching `config.yml` key. Precedence is
**environment variable → `config.yml` → built-in default**.

| Variable                 | Type                  | `config.yml` key | Default                             |
|--------------------------|-----------------------|------------------|-------------------------------------|
| `SURF_REDIS_HOST`        | string                | `host`           | `localhost`                         |
| `SURF_REDIS_PORT`        | int, `0`–`65535`      | `port`           | `6379`                              |
| `SURF_REDIS_PASSWORD`    | string (secret)       | `password`       | none (no authentication)            |
| `SURF_REDIS_CLIENT_NAME` | string                | `client-name`    | `surf-redis-client-<random UUID>`   |

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

In a surf-api plugin, the plugin context provides the `RedisApi` and manages its lifecycle (see
[Usage in a surf-api plugin](#usage-in-a-surf-api-plugin-recommended)).

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
* clients that are still held when surf-api shuts down are shut down and their
  owners logged as a warning

Redis runs only inside a surf-api runtime, which owns the process-wide networking through the
internal `RedisInstance`. surf-api provides Redis on Paper, Velocity and Minestom. Outside these
platforms, the runtime for Redis is surf-api standalone, which does not bundle the Redis
integration yet; there is no `surf-redis-standalone` module. `RedisApi.create` reads surf-api's
root context, so it works only once that context exists: not in a Paper plugin's bootstrapper.

---

## Usage in a surf-api plugin (recommended)

surf-api bundles surf-redis into its plugin on Paper, Velocity and Minestom. To compile against it, call
`withSurfRedis()` in the extension of the surf-api Gradle plugin, for example
`surfPaperPluginApi { withSurfRedis() }`. It adds `dev.slne.surf.api:surf-api-redis-api:+` to `compileOnly`, which
resolves to the newest release, like the surf-api platform dependency. Your plugin jar neither bundles nor relocates
surf-redis.

`surf-api-redis-api` is published as a shaded jar. Redisson and Netty are relocated, so import them from the
relocated packages:

```kotlin
import dev.slne.surf.redis.libs.redisson.api.RedissonClient
import dev.slne.surf.redis.libs.redisson.api.RedissonReactiveClient
import dev.slne.surf.redis.shaded.io.netty.buffer.ByteBuf
```

Every plugin context holds a managed `RedisApi`; a bean injects it and creates its sync structures and
caches while the context refreshes, in a constructor, a property initializer or a `@Bean` method.

```kotlin
@Service
class OnlineService(redis: RedisApi) {
    private val online = redis.createSyncSet<UUID>("online-players")
}
```

surf-api freezes and connects the instance right after the context refreshed, before `onLoad`, and
disconnects it when the context closes. A plugin never calls `freeze()`, `connect()` or
`disconnect()` on it, and creating a structure after the freeze throws `IllegalStateException`.
`RedissonClient` and `RedissonReactiveClient` are injectable too; their calls fail with
`IllegalStateException` while Redis is not connected. A plugin that injects none of them never starts
the Redis networking.

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
        "example:online-count",
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
        "example:players"
    )

players += "Alice"
players.remove("Bob")
```

### SyncSet

Replicated set.

```kotlin
val onlinePlayers =
    redisApi.createSyncSet<String>(
        "example:online-players"
    )

onlinePlayers += "Alice"
onlinePlayers -= "Bob"
```

### SyncMap

Replicated key-value map.

```kotlin
val playerScores =
    redisApi.createSyncMap<UUID, Int>(
        "example:scores"
    )

playerScores[playerId] = 42
```

### Custom codecs for synchronized structures

Every synchronized structure has an overload accepting a `RedisCodec`; maps accept independent key
and value codecs. The codec is used for snapshots, Redis storage, stream changes, local replication,
resynchronization, defaults, and reloads. Values go directly through the codec into a delimiter-safe
binary representation and are never converted through JSON.

```kotlin
import dev.slne.surf.redis.codec.RedisCodec
import dev.slne.surf.redis.codec.readString
import dev.slne.surf.redis.codec.readUuid
import dev.slne.surf.redis.codec.readVarInt
import dev.slne.surf.redis.codec.writeString
import dev.slne.surf.redis.codec.writeUuid
import dev.slne.surf.redis.codec.writeVarInt
import dev.slne.surf.redis.shaded.io.netty.buffer.ByteBuf
import java.util.UUID

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
    id = "example:player-states",
    keyCodec = UUIDBinaryCodec,
    valueCodec = PlayerStateCodec
)

val queue = redisApi.createSyncList(
    id = "example:player-queue",
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
    redisApi.createSimpleSetRedisCache<Player>(
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
./gradlew :surf-api-redis:surf-api-redis-core:lincheckTest
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

All sync structures (`SyncValue`, `SyncList`, `SyncSet`, `SyncMap`) and caches **must be created
before** `RedisApi.freeze()` is called. Creating one afterwards throws an exception.

In a surf-api plugin, surf-api freezes the plugin's `RedisApi` right after the context refreshed.
Create structures in a constructor, a property initializer or a `@Bean` method of a singleton bean,
which all run during the refresh. A bean created later, for example a lazy bean or one obtained
through an `ObjectProvider` after the start, cannot create structures.

When you call `freezeAndConnect()` yourself, note that structures are often created in services that depend on
Redis rather than in the code that owns the `RedisApi`, so initialize every such service first:

```kotlin
class RedisSetup {

    val redisApi = RedisApi.create("my-app")

    fun connect() {
        // Initialize every service that creates sync structures first.
        SomeService.init(redisApi)
        AnotherService.init(redisApi)

        redisApi.freezeAndConnect()
    }
}
```

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
