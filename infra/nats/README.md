# surf-nats

[![Version](https://img.shields.io/badge/version-1.0.0--SNAPSHOT-informational)](https://github.com/SLNE-Development/surf-nats)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-coroutines-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![NATS](https://img.shields.io/badge/NATS-JetStream-27AAE1?logo=natsdotio)](https://nats.io)
[![jnats](https://img.shields.io/badge/jnats-2.26.4-27AAE1)](https://github.com/nats-io/nats.java)
[![Platforms](https://img.shields.io/badge/platforms-Paper%20%7C%20Velocity%20%7C%20Minestom%20%7C%20Standalone-lightgrey)](#installation)
[![Last commit](https://img.shields.io/github/last-commit/SLNE-Development/surf-nats)](https://github.com/SLNE-Development/surf-nats/commits)

surf-nats connects surf plugins and microservices over [NATS](https://nats.io). It provides two things:

- **Events**: publish a Kotlin object, and every instance that listens for it receives it.
- **Services**: declare a Kotlin interface, implement it on one instance, and call it from another like a
  local `suspend` function.

Two terms recur throughout this README. An *application* is a plugin or a microservice. An *instance* is
one running copy of an application: three Paper servers running your plugin are three instances of one
application.

surf-nats replaces the pub/sub of surf-redis and the request/response system of surf-rabbitmq. Messages
are serialized with kotlinx.serialization, as CBOR by default or as JSON. Service calls can be made
durable with JetStream, and large arguments and results are moved through the NATS object store
automatically.

surf-nats requires Java 25.

## Installation

Call `withSurfNats()` in the surf-api Gradle plugin. On Paper and Velocity, this also adds the
dependency on the surf-nats plugin.

On Paper, Velocity, and Minestom, the surf-nats plugin opens the connection at startup and closes it at
shutdown. Your plugin only creates and closes its own clients.

A microservice manages the connection itself. Start surf-nats in `onBootstrap` before you create a client,
and shut it down in `onDisable` after you close it:

```kotlin
@AutoService(Microservice::class)
class ProfileMicroservice : Microservice() {
    override val dataPath: Path = Path("data")

    override suspend fun onBootstrap(args: List<String>) {
        SurfNatsStandalone.start(dataPath.resolve("nats"))
        // Create the NatsClient and register listeners and services.
    }

    override suspend fun onDisable() {
        // Close the NatsClient.
        SurfNatsStandalone.shutdown()
    }
}
```

surf-nats writes its `config.yml` into the directory passed to `start`. Give surf-nats a directory of its
own, so the file does not collide with other configuration.

You need a NATS server with JetStream enabled. For local development, start one with the
`docker-compose.yml` in this repository:

```sh
docker compose up -d
```

## Configuration

surf-nats reads `config.yml` from its data directory: the plugin folder on Paper, Velocity, and Minestom,
or the directory passed to `SurfNatsStandalone.start`.

```yaml
servers:
  - nats://localhost:4222
auth:
  username: profiles
  password: change-me
reconnect-wait-millis: default
drain-timeout-seconds: default
```

`default` uses the default value from the table below.

| Key | Meaning | Default |
| --- | --- | --- |
| `servers` | NATS server URLs. At least one is required. | `nats://localhost:4222` |
| `auth` | One of `token`, `username` with `password`, or `credentials-file`. | none |
| `connection-name` | Name of the connection shown by the NATS server. | `surf-nats-<random id>` |
| `reconnect-wait-millis` | Pause between reconnect attempts. | 2000 |
| `drain-timeout-seconds` | Time that closing a service waits for running calls. | 10 |

Environment variables override the file:

| Variable | Overrides |
| --- | --- |
| `SURF_NATS_SERVERS` | `servers`, comma-separated |
| `SURF_NATS_TOKEN` | `auth.token` |
| `SURF_NATS_USERNAME`, `SURF_NATS_PASSWORD` | `auth.username`, `auth.password` |
| `SURF_NATS_CREDENTIALS_FILE` | `auth.credentials-file` |
| `SURF_NATS_CONNECTION_NAME` | `connection-name` |

If any of `SURF_NATS_TOKEN`, `SURF_NATS_USERNAME`, `SURF_NATS_PASSWORD`, or `SURF_NATS_CREDENTIALS_FILE`
is set, the environment replaces the whole `auth` section. A username from the environment never combines
with a password from the file.

The NATS server must be reachable at startup. Once connected, surf-nats reconnects indefinitely after a
connection loss.

## Clients

A `NatsClient` is your application's handle to NATS. Create one per plugin or microservice and close it
when the application shuts down:

```kotlin
val client = NatsClient.create("profiles")

// On shutdown:
client.close()
```

The client name identifies your application. It consists of lowercase letters and digits, separated by
hyphens. All instances of an application use the same name. Within one JVM, the name must be unique.

The name scopes events and services. By default, an event published by `profiles` reaches only instances
whose client is named `profiles`, and a service registered by `profiles` can only be called by `profiles`.
Mark an event or service as `global` to share it with other applications.

`NatsClient.create` takes an optional `SerializersModule` for contextual and polymorphic serializers. This
module extends the serializers module from surf-api and overrides it where both define a serializer.

`close()` closes every listener and service registration of the client. A closed client cannot be reused.
After the surf-nats plugin or `SurfNatsStandalone` restarts, create a new client.

## Events

An event is a class annotated with `@NatsEvent`. The annotation makes the class serializable, so it needs
no `@Serializable`.

```kotlin
// subject is optional. Without it, surf-nats derives it from the class name: "name-changed".
@NatsEvent(subject = "name-changed")
data class NameChanged(val playerId: String, val name: String)
```

Publish an event with `publish`:

```kotlin
client.publish(NameChanged("alice", "Alice"))
```

`publish` does not suspend. It hands the event to the connection and returns. It does not wait for
delivery, and delivery is not guaranteed: an instance that is offline misses the event. An event must fit
into a single NATS message, whose size the server limits with `max_payload` (1 MB by default). For a
larger event, `publish` throws an `IllegalArgumentException`.

### Listening

A listener method is a method annotated with `@NatsListener`. It takes the event as its only parameter,
returns `Unit`, and may be `suspend`. You register the object that holds listener methods:

```kotlin
object NameListener {
    @NatsListener
    suspend fun onNameChanged(event: NameChanged) {
        println("${event.playerId} is now ${event.name}")
    }
}

client.registerListener(NameListener)
```

`registerListener` returns once the NATS server knows the subscription. Every event published after that
reaches the listener methods. `registerListener` returns a `NatsRegistration`. Close it to stop listening,
or let `client.close()` close it.

`registerListener` checks every listener method and reports all problems at once. Listener methods in
superclasses are registered too. A method that overrides a listener method must repeat `@NatsListener`.

A listener method that throws does not affect other listener methods. surf-nats logs the exception.

### Recipients

By default, every instance of the client receives every event (`Recipients.ALL_INSTANCES`). This is the
behavior of surf-redis.

With `Recipients.ANY_INSTANCE`, each event goes to exactly one instance of the client. Use it to spread
work across instances:

```kotlin
@NatsListener(recipients = Recipients.ANY_INSTANCE)
fun onNameChanged(event: NameChanged) {
    // Runs on one instance only.
}
```

### Own events

A client receives its own events, as with surf-redis. To skip events that this client published in this
JVM, set `ignoreFromThisClient`:

```kotlin
@NatsListener(ignoreFromThisClient = true)
fun onNameChanged(event: NameChanged) { }
```

A different client in the same JVM counts as a different publisher. `ignoreFromThisClient` works only with
`ALL_INSTANCES`.

### Event origin

A `suspend` listener method can read where the event came from:

```kotlin
@NatsListener
suspend fun onNameChanged(event: NameChanged) {
    val context = natsEventContext()
    println("Published by ${context.origin}, own event: ${context.isFromThisClient}")
}
```

`origin` has the form `<client name>@<instance id>`, where the instance id is a random id per JVM. It is
`null` for events from publishers outside surf-nats.

### Subjects and global events

NATS delivers each message by its subject, an address such as `profiles.name-changed`. surf-nats builds
the NATS subject of an event from the client name, a dot, and the `subject` value of `@NatsEvent`. The
event `NameChanged` published by client `profiles` therefore travels on `profiles.name-changed`. Instances
of client `profiles` subscribe to that NATS subject. Instances of client `lobby` subscribe to
`lobby.name-changed` and never see the event.

Set `global = true` when other applications publish or receive the event: another plugin with a different
client name, a microservice, or an application outside surf-nats such as a web panel. A global event
travels on its `subject` value alone, without the client name:

```kotlin
@NatsEvent(subject = "player.name-changed", global = true)
data class NameChanged(val playerId: String, val name: String)
```

Do not start a global subject with a client name. `profiles.name-changed` as a global subject would mix with
the events of client `profiles`.

The derived subject changes when you rename the class. Moving the class to another package does not change
it. For events that other applications use, set `subject` explicitly.

### Event format

Events are sent as CBOR. Annotate the event class with `@NatsFormat(WireFormat.JSON)` to send JSON
instead, for example when a web application consumes it. Receivers read either format, so publishers and
listeners do not need to agree on it.

## Services

A service is a Kotlin interface annotated with `@NatsService`. An instance registers an implementation,
and other instances call it through a generated proxy. By default, both sides belong to the same
application (see [Global services](#global-services) for calls between applications).

A call works like an event with a reply. The proxy publishes the call as a NATS message, an instance that
listens for it runs the method, and the result travels back as another message. Like events, NATS does not
store these messages. If no instance is running, the call reaches nobody, and if the instance stops while
it runs the method, the result never arrives. Depending on the method, the caller then gets an
exception (see [Errors](#errors)) or `Answer.Unknown`. [Persistent methods](#persistent-methods) store calls in JetStream instead.

```kotlin
@NatsService("profiles")
interface ProfileService {
    suspend fun name(playerId: String): String?
    suspend fun rename(playerId: String, name: String): RenameResult
}
```

Every method must be `suspend`. Parameters and return types must be serializable. Value classes such as
`ULong`, `Duration`, or your own `@JvmInline` classes work as parameters.

Register the implementation on the instances that serve the calls (`RenameResult` is defined under
[Errors](#errors)):

```kotlin
object ProfileServiceImpl : ProfileService {
    override suspend fun name(playerId: String): String? = repository.findName(playerId)
    override suspend fun rename(playerId: String, name: String): RenameResult = repository.rename(playerId, name)
}

client.registerService<ProfileService>(ProfileServiceImpl)
```

Call it from any instance of the same application:

```kotlin
val profiles = client.service<ProfileService>()
val name = profiles.name("alice")
```

`service()` returns the same proxy every time for a given interface, so you can fetch it once and keep it.

### Global services

Like events, services are scoped to the client name. Set `global = true` on `@NatsService` to let other
applications call the service or serve it. All implementations of a global service share its calls,
whichever client registered them. Two different global services must therefore not share a name, or
their calls would mix.

### What a call does

What a call does depends on three things: whether the method is persistent (`@NatsPersistent`, see
[Persistent methods](#persistent-methods)), which instances it addresses (`@NatsRecipients`), and its
return type. `Answer<T>` is explained under [Asking all instances](#asking-all-instances).

| Persistent | Recipients | Returns | The call |
| --- | --- | --- | --- |
| no | `ANY_INSTANCE` | a value | sends the call to one instance and waits for its result |
| no | `ANY_INSTANCE` | `Unit` | sends the call to one instance and waits until it has run the method |
| no | `ALL_INSTANCES` | `Unit` | sends the call to every instance and returns immediately, without waiting for any instance to run the method (fire and forget) |
| no | `ALL_INSTANCES` | `Answer<T>` | sends the call to every instance and returns the first known answer |
| yes | `ANY_INSTANCE` | `Unit` | returns once JetStream has stored the call, which one instance runs later |
| yes | `ANY_INSTANCE` | a value | stores the call, which one instance runs, and waits for its result |

`ANY_INSTANCE` is the default. With `ANY_INSTANCE`, NATS picks one of the running instances and delivers the
call to that instance only. The other instances never see it, so only that instance runs the method. With
`ALL_INSTANCES`, every running instance receives the call and runs the method.

Persistent methods cannot use `ALL_INSTANCES`, and `ALL_INSTANCES` methods cannot return a plain value.
Changing the return type changes what a call does.

Set `@NatsRecipients`, `@NatsPersistent`, `@NatsCallTimeout`, or `@NatsFormat` on the interface to apply
it to all methods. The same annotation on a method overrides the one on the interface.

### Asking all instances

`ALL_INSTANCES` with `Answer<T>` answers questions such as "on which server is this player?". Every
instance answers with `Answer.Known(value)` or `Answer.Unknown`:

```kotlin
@NatsService("players")
interface PlayerService {
    @NatsRecipients(Recipients.ALL_INSTANCES)
    suspend fun findServer(playerId: String): Answer<String>
}

object PlayerServiceImpl : PlayerService {
    override suspend fun findServer(playerId: String): Answer<String> =
        if (isOnline(playerId)) Answer.Known(serverName) else Answer.Unknown
}

val players = client.service<PlayerService>()
when (val answer = players.findServer("alice")) {
    is Answer.Known -> println("alice is on ${answer.value}")
    Answer.Unknown -> println("alice is offline")
}
```

The first `Known` answer ends the call. The call returns `Unknown` once every instance has answered
`Unknown`, when no instance serves the method, or when the timeout expires. If no instance answers `Known`
and one of them threw, the call throws that exception.

surf-nats tracks which instances are running, so a call can end as soon as all of them have answered. For
about one second after the first `service()` call and after a reconnect, it does not know the instances
yet. A call in that window that gets no `Known` answer waits for its timeout.

### Timeouts

A call waits 5 seconds by default, a persistent call 60 seconds. `@NatsCallTimeout` on the interface or
on a method changes the timeout. `withNatsCallTimeout` overrides it for every call inside the block:

```kotlin
@NatsCallTimeout(seconds = 2)
suspend fun name(playerId: String): String?

val name = withNatsCallTimeout(500.milliseconds) {
    profiles.name("alice")
}
```

A persistent call first waits for JetStream to confirm that it stored the call. While it waits, surf-nats
sends the call again whenever the confirmation does not arrive, each time with the same message id.
JetStream discards a message whose id it has already seen, but only within the last two minutes, the
duplicate window that surf-nats sets on its streams. A repeat sent later would be stored as a second call,
so surf-nats waits for the confirmation for at most two minutes.

For a persistent method without a result, the confirmation is all the call waits for, so its timeout can
be at most two minutes. surf-nats rejects a longer `@NatsCallTimeout` in `registerService` and `service()`,
and a longer `withNatsCallTimeout` at the call. A persistent method with a result waits for the
confirmation for at most two minutes and then for the result until its full timeout, so its timeout can be
longer, for example five minutes.

### Errors

Calls throw `NatsRpcException` or one of its subclasses:

| Exception | Cause |
| --- | --- |
| `NatsNoRespondersException` | No instance serves the method. Thrown at once, only for non-persistent `ANY_INSTANCE` calls. |
| `NatsCallTimeoutException` | No result or storage confirmation within the timeout. Never thrown by `ALL_INSTANCES` calls. |
| `NatsRemoteException` | The implementation threw. Carries the exception's class name and message, without stack trace or cause. |
| `NatsRpcException` | The NATS server rejected a persistent call. |

A timeout does not mean that the method did not run. Design writes so that a retry is safe.

Expected outcomes belong in the return type, not in exceptions. Return a sealed result for cases such as
"name already taken", and reserve exceptions for bugs:

```kotlin
@Serializable
sealed interface RenameResult {
    @Serializable data object Renamed : RenameResult
    @Serializable data object NameTaken : RenameResult
}
```

### Names

surf-nats derives the service name from the interface name, the method names from the Kotlin method
names, and the argument names from the parameter names. These names are part of the protocol: renaming a
method breaks callers that still use the old name.

When a service has callers outside your codebase, or callers that update at a different time, set every
name explicitly: the service name in `@NatsService`, each method with `@NatsMethod`, and each parameter
with `@NatsParam`.

```kotlin
@NatsService("profiles", global = true)
interface ProfileService {
    @NatsMethod("rename")
    suspend fun rename(@NatsParam("playerId") playerId: String, @NatsParam("name") name: String): RenameResult
}
```

### Call format

Proxies send CBOR by default. `@NatsFormat(WireFormat.JSON)` on the interface or a method switches to JSON.
The implementation answers in the format of the call.

## Persistent methods

A persistent method stores each call in JetStream before an instance runs it. The call survives restarts:
if no instance is running, it waits until one starts. Use persistent methods for writes that must not get
lost. Keep reads non-persistent, so that they fail fast when the service is down.

```kotlin
@NatsService("profiles")
interface ProfileService {
    @NatsPersistent
    suspend fun rename(playerId: String, name: String)

    suspend fun name(playerId: String): String?
}
```

To make all methods persistent except a few, annotate the interface with `@NatsPersistent` and the
exceptions with `@NatsPersistent(enabled = false)`.

### Calls without a result

A persistent method that returns `Unit` returns as soon as JetStream has stored the call. One instance runs
it later. If the implementation throws, surf-nats retries after 1 second, 5 seconds, 30 seconds, and
2 minutes. After the fifth failed attempt (the first attempt and four retries), surf-nats moves the call
to the service's dead-letter stream, where it waits for manual inspection (see
[Operations](#operations)). A call that cannot be decoded goes to the dead-letter stream at once.

While the implementation runs, surf-nats tells JetStream every 5 seconds that the call is still in
progress. Without these signals, JetStream would assume after 10 seconds that the instance had crashed and
deliver the call again. The signals have a downside: a method that never returns would hold its call
forever, and JetStream would neither retry it nor move it to the dead-letter stream. surf-nats therefore
cancels the method after 10 minutes and counts the attempt as failed, so the call is retried or
dead-lettered like any other failure.

The cancellation is a coroutine cancellation, which stops the method only at its next suspension point.
Blocking code, such as a JDBC query, does not notice it and keeps running, and the signals to JetStream
continue with it. Wrap blocking code in `runInterruptible(Dispatchers.IO)`, which interrupts the thread
when the method is cancelled.

### Calls with a result

A persistent method that returns a value stores the call and waits for the result. If the implementation
throws, the caller receives a `NatsRemoteException`, and surf-nats does not retry.

### Running a call more than once

JetStream delivers each call at least once. If an instance crashes before it confirms a call, JetStream
delivers the call again, so the implementation must tolerate running twice. `natsPersistentContext()` tells the
implementation which delivery it handles:

```kotlin
override suspend fun rename(playerId: String, name: String) {
    val context = natsPersistentContext()
    println("message ${context.messageId}, attempt ${context.deliveryCount}")
}
```

`messageId` stays the same across retries of one call. Two calls with the same arguments get different ids,
so `messageId` cannot detect an operation that a caller sends twice. Use a key from your domain for that,
such as an operation id passed as a parameter.

### Deadlines

Each persistent call with a result carries a deadline: the time of the call plus its timeout. An instance
drops calls whose deadline has passed, because the caller no longer waits for the result. The deadline is
an absolute time, so all hosts need synchronized clocks (NTP).

### Streams

`registerService` creates the JetStream stream and consumer of a persistent service. A Kotlin caller also
creates the stream on its first call, so calls are stored even before any instance has registered.
A caller outside surf-nats cannot do that: its calls fail until the service has been registered once.

## Large payloads

The NATS server limits the size of a message with `max_payload`, 1 MB by default. When the arguments or
the result of a service call exceed that limit, surf-nats stores them in the NATS object store and sends a reference instead. The receiver
loads them transparently. surf-nats deletes the objects after use, and objects it does not delete expire
after the time in the table below. Your code passes ordinary values:

```kotlin
suspend fun upload(data: ByteArray)
```

| Bucket | Holds | Kept for |
| --- | --- | --- |
| `surf-nats-payloads` | arguments and results of non-persistent calls and persistent calls with a result | 10 minutes |
| `surf-nats-persistent-payloads` | arguments of persistent calls without a result | 7 days |

The object store needs JetStream and a `max_payload` of at least 128 KB. Events never use it.

Objects in `surf-nats-payloads` expire after 10 minutes, whatever the timeout of the call. A call with a
longer timeout therefore fails when its arguments or result need the object store: arguments with an
`IllegalArgumentException`, a result with a `NatsRemoteException`. Calls with small payloads are not
affected.

## Threads

surf-nats runs listeners and service implementations concurrently on `Dispatchers.Default`, in no
particular order. Do not block there. Move blocking work to `Dispatchers.IO`:

```kotlin
override suspend fun name(playerId: String): String? = withContext(Dispatchers.IO) {
    database.findName(playerId)
}
```

Listener methods and service implementations do not run on the server's main thread. Use the platform
scheduler for Paper, Velocity, or Minestom APIs that must run on the main thread.

`NatsRegistration.close()` and `NatsClient.close()` block the calling thread while services finish their
running calls, for up to `drain-timeout-seconds`. Running listener methods are not waited for.

## Operations

### Streams and dead letters

Each persistent service has a stream for its calls and a dead-letter stream for calls that failed for good.
The stream is named `SURF_<CLIENT>_<SERVICE>`, or `SURF__<SERVICE>` for a global service, in upper case with
hyphens replaced by underscores. The dead-letter stream appends `_DLQ`:

| Service | Stream | Dead-letter stream |
| --- | --- | --- |
| `profiles` of client `player-data` | `SURF_PLAYER_DATA_PROFILES` | `SURF_PLAYER_DATA_PROFILES_DLQ` |
| global `profiles` | `SURF__PROFILES` | `SURF__PROFILES_DLQ` |

Both streams keep messages for 7 days. surf-nats does not process the dead-letter stream. Inspect it with
the `nats` CLI, fix the cause, and publish the calls to the service again:

```sh
nats stream view SURF_PLAYER_DATA_PROFILES_DLQ
```

[External callers](docs/external-callers.md) describes the message format for publishing a call again.

### Known limits

- A call can stay in the stream without further attempts until it expires after 7 days. This happens when
  moving it to the dead-letter stream fails, or when the instance crashes during the last attempt.
- A call within about 150 bytes of `max_payload` does not fit into the dead-letter stream, because
  surf-nats adds headers to it. It stays in the stream until it expires.
- A dead-lettered call whose arguments live in the object store can only be published again within 7 days
  of the original call.
- If a stream is deleted, restart every instance that registers or calls the service. surf-nats does not
  recreate deleted streams or consumers while running.
- Changing `global` on a service creates a new stream. The old stream and its unprocessed calls stay behind.
- Instances that share a consumer use the consumer settings of the instance that registered last. Keep
  versions aligned during rolling updates.
- Repeated warnings about expired calls can point to clock skew between hosts (see
  [Deadlines](#deadlines)).

## External callers

Applications outside surf-nats, such as a web panel, can publish events and call services. A persistent
method without a result fits commands that need no answer. Make the events and services `global`, set
every name explicitly (see [Names](#names)), and register the service once before the external
application publishes calls.

[External callers](docs/external-callers.md) describes subjects, headers, content types, and the JSON
format.

## Migrating from surf-redis and surf-rabbitmq

| surf-redis / surf-rabbitmq | surf-nats |
| --- | --- |
| `@Serializable class X : RedisEvent()` | `@NatsEvent class X` |
| `@OnRedisEvent` | `@NatsListener` |
| `subscribeToEvents(listener)` | `client.registerListener(listener)` |
| `publishEvent(event).await()` | `client.publish(event)` |
| Request and response packet | One `suspend` method of a `@NatsService` interface. Request fields become parameters, the response becomes the return type. |
| `@RabbitHandler` | The implementation of that method |
| `sendRequest(packet)` | A call on `client.service<T>()` |

Points that behave differently:

- **Requests are no longer persistent by default.** surf-rabbitmq stored requests in a durable queue for up
  to 60 seconds. A non-persistent surf-nats call fails at once with `NatsNoRespondersException` when no
  instance runs. Mark methods with side effects `@NatsPersistent`. Keep reads non-persistent.
- **One method per request type.** Replace packets that switch on a type field with one method per type,
  each with its own return type.
- **Event subjects come from the class name.** Renaming an event class changes its subject. Set `subject`
  for events that other applications use.
- **Commands that only some instances can handle become services.** A message such as "send this player a
  message", which only the server the player is on can carry out, belongs in a service method with
  `ALL_INSTANCES`: `Unit` to send it to every instance, or `Answer<T>` to learn which
  instance handled it. Every instance receives the call and decides whether it is responsible.
- **Exceptions arrive as `NatsRemoteException`.** Return expected failures as sealed results.

## Modules

| Module | Contents |
| --- | --- |
| `surf-nats-api` | The public API: client, annotations, contexts, `Answer`, exceptions |
| `surf-nats-core` | The implementation |
| `surf-nats-paper` | The Paper plugin |
| `surf-nats-velocity` | The Velocity plugin |
| `surf-nats-minestom` | The Minestom plugin |
| `surf-nats-standalone` | `SurfNatsStandalone` for microservices |
