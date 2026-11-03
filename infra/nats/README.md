# surf-nats

[![Version](https://img.shields.io/badge/version-1.0.0--SNAPSHOT-informational)](https://github.com/SLNE-Development/surf-nats)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-coroutines-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Protobuf](https://img.shields.io/badge/Protobuf-proto2-4285F4)](https://protobuf.dev/)
[![NATS](https://img.shields.io/badge/NATS-JetStream-27AAE1?logo=natsdotio)](https://nats.io)
[![jnats](https://img.shields.io/badge/jnats-2.26.4-27AAE1)](https://github.com/nats-io/nats.java)
[![Platforms](https://img.shields.io/badge/platforms-Paper%20%7C%20Velocity%20%7C%20Minestom%20%7C%20Standalone-lightgrey)](#installation)
[![Last commit](https://img.shields.io/github/last-commit/SLNE-Development/surf-nats)](https://github.com/SLNE-Development/surf-nats/commits)

surf-nats connects surf plugins and microservices over [NATS](https://nats.io). It provides two things:

- **Events**: publish a message, and every instance that listens for it receives it.
- **Services**: declare a service, implement it on one instance, and call it from another like a local
  `suspend` function.

Two terms recur throughout this README. An *application* is a plugin or a microservice. An *instance* is one
running copy of an application: three Paper servers running your plugin are three instances of one application.

Events and services are declared in `.proto` files, the *contracts*. The surf-nats Gradle plugin generates Kotlin
classes and interfaces from them, and messages travel in the binary [Protocol Buffers](https://protobuf.dev/)
format (proto2). Applications in other languages read the same contracts (see
[External callers](#external-callers)).

surf-nats replaces the pub/sub of surf-redis and the request/response system of surf-rabbitmq. Service calls can be
made durable with JetStream, and large arguments and results are moved through the NATS object store
automatically.

surf-nats requires Java 25.

## Installation

Apply the Gradle plugin `dev.slne.surf.nats` in every module that uses surf-nats or contains contracts. The plugin
and the surf-nats libraries share one version:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://reposilite.slne.dev/public/")
    }
}

// build.gradle.kts
plugins {
    id("dev.slne.surf.nats") version "<surf-nats version>"
}
```

The plugin supports Kotlin/JVM modules and needs surf-api 3.47.1 or later, and surf-microservice 2.1.2 or later for
microservices. Next to any surf-api Gradle plugin, it does the following:

- It adds `compileOnly` dependencies on `surf-nats-api` and the type adapters in `surf-nats-types-common`, plus
  `surf-nats-types-paper` in Paper modules, and on `kotlinx-serialization-protobuf`. At runtime, the surf-nats
  platform plugin provides them.
- It declares the surf-nats platform plugin as a dependency of your plugin: on Paper as a required bootstrap and
  server dependency in `paper-plugin.yml`, on Velocity in the plugin file.
- It applies the Kotlin serialization compiler plugin.
- It applies the weaver Gradle plugin `dev.slne.weaver` and configures it (see
  [Contract registry](#contract-registry)).

Applying the plugin is enough. Do not register the surf-nats plugin as a dependency yourself, because the build
would declare it twice. On Minestom, register the dependency in `minestomPluginFile`, which the plugin does not do:

```kotlin
minestomPluginFile {
    pluginDependencies {
        register("surf-nats-minestom")
    }
}
```

On Paper, Velocity, and Minestom, the surf-nats plugin opens the connection at startup and closes it at shutdown.
Your plugin only creates and closes its own clients.

In a microservice, apply the plugin next to `surfMicroservice { withMicroserviceApi() }`. The microservice runtime
then starts surf-nats before `onBootstrap` and shuts it down after `onDisable`. Your microservice only creates and
closes its own clients. Do not call `SurfNatsStandalone` yourself.

You need a NATS server with JetStream enabled. For local development, start one with the `docker-compose.yml` in
this repository:

```sh
docker compose up -d
```

## Configuration

surf-nats reads `config.yml` from its data directory: the plugin folder on Paper, Velocity, and Minestom, or
`<dataPath>/nats` in a microservice.

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

| Key                     | Meaning                                                            | Default                 |
|-------------------------|--------------------------------------------------------------------|-------------------------|
| `servers`               | NATS server URLs. At least one is required.                        | `nats://localhost:4222` |
| `auth`                  | One of `token`, `username` with `password`, or `credentials-file`. | none                    |
| `connection-name`       | Name of the connection shown by the NATS server.                   | `surf-nats-<random id>` |
| `reconnect-wait-millis` | Pause between reconnect attempts.                                  | 2000                    |
| `drain-timeout-seconds` | Time that closing a service waits for running calls.               | 10                      |

Environment variables override the file:

| Variable                                   | Overrides                        |
|--------------------------------------------|----------------------------------|
| `SURF_NATS_SERVERS`                        | `servers`, comma-separated       |
| `SURF_NATS_TOKEN`                          | `auth.token`                     |
| `SURF_NATS_USERNAME`, `SURF_NATS_PASSWORD` | `auth.username`, `auth.password` |
| `SURF_NATS_CREDENTIALS_FILE`               | `auth.credentials-file`          |
| `SURF_NATS_CONNECTION_NAME`                | `connection-name`                |

If any of `SURF_NATS_TOKEN`, `SURF_NATS_USERNAME`, `SURF_NATS_PASSWORD`, or `SURF_NATS_CREDENTIALS_FILE` is set,
the environment replaces the whole `auth` section. A username from the environment never combines with a password
from the file.

The NATS server must be reachable at startup. Once connected, surf-nats reconnects indefinitely after a connection
loss.

## Contracts

A contract is a `.proto` file under `src/main/proto`. Its directory matches its package:
`src/main/proto/surf/profiles/v1/name_changed.proto` declares `package surf.profiles.v1`. Every contract starts like
this:

```proto
syntax = "proto2";

package surf.profiles.v1;

option java_package = "dev.slne.surf.profiles.proto.v1";

import "surf/nats/v1/options.proto";
```

- Only proto2 is supported. A file without a `syntax` line counts as proto2.
- A file that declares events or services needs a package ending in `.v<N>`, the major version of the contract
  (see [Versioning](#versioning)). The Kotlin package, taken from `java_package` or else from the proto package,
  must end in the same `.v<N>`. Use `java_package = "<base>.proto.v<N>"` and keep only generated code in that
  package.
- Files that use surf-nats options import `surf/nats/v1/options.proto` directly.
- Put each event and each shared message in its own file. A service may share its file with its own request and
  response messages.

Keep contracts in a module that both sides use, such as the common module of a plugin and its microservice, but
not in your public API module. The generated classes are internal to your project: other plugins use your plugin's
Kotlin API. The module publishes its contracts to the weaver registry, so other modules and applications in other
languages can read them (see [Contract registry](#contract-registry)).

The plugin generates the Kotlin sources into `build/generated/surf-nats/kotlin` with the task
`generateSurfNatsKotlin`. Read the generated signatures there before you write the code that uses them.

### Fields

Every message becomes a Kotlin `data class`, and every field a property in `lowerCamelCase`:

- Declare all fields `optional`. `required` is rejected.
- A field is non-null in Kotlin. A message that lacks a non-null field fails to decode. Mark a field
  `[(surf.nats.v1.nullable) = true]` to make it `T? = null`, or give it a `[default = …]`, which becomes its Kotlin
  default value.
- `repeated` becomes `List<T> = emptyList()`, and `map<K, V>` becomes `Map<K, V> = emptyMap()`.
- A `oneof` becomes a nullable property whose type is a `sealed interface` with one `data class` per case. Each case
  class holds its value in `value`.
- `int32`, `fixed32`, and `sfixed32` become `Int`. `int64`, `sint64`, `fixed64`, and `sfixed64` become `Long`.
  `uint32` and `uint64` become `Int` and `Long` in two's complement, so values of 2^31 or 2^63 and above are
  negative in Kotlin. `sint32` is rejected; use `sint64` or `int32`.
- `bytes` becomes `ByteArray`. The generated `equals` and `hashCode` compare its content.
- Constructor parameters follow the field numbers, not the order in the file. Give new fields a number above the
  current maximum, and call generated constructors with named arguments.

Field numbers identify fields on the wire. Never change or reuse them. Reserve the numbers and names of deleted
fields with `reserved`.

### Enums

Every value of an enum starts with the enum name in upper snake case, and the first value is `<PREFIX>_UNSPECIFIED
= 0` or `<PREFIX>_UNKNOWN = 0`. Kotlin drops the prefix:

```proto
enum PunishmentType {
  PUNISHMENT_TYPE_UNSPECIFIED = 0;
  PUNISHMENT_TYPE_BAN = 1;
  PUNISHMENT_TYPE_MUTE = 2;
}
```

This enum becomes `PunishmentType.UNSPECIFIED`, `PunishmentType.BAN`, and `PunishmentType.MUTE`. Write enum names
in upper camel case with abbreviations as one word (`HttpStatus`, not `HTTPStatus`). protobuf-es derives a
different prefix from `HTTPStatus` and would keep the full value names in TypeScript, so the plugin rejects such
names. A number that the reader does not know decodes to the zero value, also inside lists and maps.

### Types

Use these types instead of primitives where they fit. The plugin maps them to Kotlin types:

| Proto type (import)                                                    | Kotlin type                                               |
|------------------------------------------------------------------------|-----------------------------------------------------------|
| `google.protobuf.Timestamp` (`google/protobuf/timestamp.proto`)        | `java.time.Instant`                                       |
| `google.protobuf.Duration` (`google/protobuf/duration.proto`)          | `kotlin.time.Duration`                                    |
| `google.protobuf.Empty` (`google/protobuf/empty.proto`)                | `Unit`, only as the request or response of an `rpc`       |
| `surf.types.v1.Uuid` (`surf/types/v1/uuid.proto`)                      | `java.util.UUID`                                          |
| `surf.types.v1.Component` (`surf/types/v1/component.proto`)            | `net.kyori.adventure.text.Component`                      |
| `surf.types.v1.Key` (`surf/types/v1/key.proto`)                        | `net.kyori.adventure.key.Key`                             |
| `surf.types.v1.TextColor` (`surf/types/v1/text_color.proto`)           | `net.kyori.adventure.text.format.TextColor`               |
| `surf.types.v1.ShadowColor` (`surf/types/v1/shadow_color.proto`)       | `net.kyori.adventure.text.format.ShadowColor`             |
| `surf.types.v1.Sound` (`surf/types/v1/sound.proto`)                    | `net.kyori.adventure.sound.Sound`                         |
| `surf.types.v1.Title` (`surf/types/v1/title.proto`)                    | `net.kyori.adventure.title.Title`                         |
| `surf.types.v1.OffsetDateTime` (`surf/types/v1/offset_date_time.proto`) | `java.time.OffsetDateTime`                               |
| `surf.types.v1.ZonedDateTime` (`surf/types/v1/zoned_date_time.proto`)  | `java.time.ZonedDateTime`                                 |
| `surf.types.v1.LocalDate` (`surf/types/v1/local_date.proto`)           | `java.time.LocalDate`                                     |
| `surf.types.v1.LocalTime` (`surf/types/v1/local_time.proto`)           | `java.time.LocalTime`                                     |
| `surf.types.v1.LocalDateTime` (`surf/types/v1/local_date_time.proto`)  | `java.time.LocalDateTime`                                 |
| `surf.types.v1.BigDecimal` (`surf/types/v1/big_decimal.proto`)         | `java.math.BigDecimal`                                    |
| `surf.types.v1.BigInteger` (`surf/types/v1/big_integer.proto`)         | `java.math.BigInteger`                                    |
| `surf.types.v1.IpAddress` (`surf/types/v1/ip_address.proto`)           | `java.net.InetAddress`                                    |
| `surf.types.v1.InetSocketAddress` (`surf/types/v1/inet_socket_address.proto`) | `java.net.InetSocketAddress`                       |
| `surf.types.paper.v1.Location` (`surf/types/paper/v1/location.proto`)  | `org.bukkit.Location`, in Paper modules only              |
| `surf.types.paper.v1.ItemStack` (`surf/types/paper/v1/item_stack.proto`) | `org.bukkit.inventory.ItemStack`, in Paper modules only |

To map a message of your own contract to an existing class, declare the message and register a `KSerializer` whose
descriptor matches it:

```kotlin
surfNats {
    mapType("surf.profiles.v1.Tag", "dev.slne.surf.profiles.Tag", "dev.slne.surf.profiles.TagSerializer")
}
```

Events cannot be mapped. `google.protobuf.Any` and other types without a mapping are not supported; use a `oneof`
instead.

### Plugin options

All entries of the `surfNats { }` block are optional:

| Entry                                     | Effect                                                                                    |
|-------------------------------------------|-------------------------------------------------------------------------------------------|
| `protoSourceDirs`                         | Directories with contracts to generate. Default `src/main/proto`.                         |
| `protoPath.from(...)`                     | Local directories with `.proto` files that contracts may import, but that are not generated. |
| `mapType(proto, kotlinClass, serializer)` | Maps a proto type to an existing class (see [Types](#types)).                             |
| `generatedVisibility = INTERNAL`          | Generates `internal` classes, interfaces, and enums. Default `PUBLIC`.                    |
| `compatibilityBaseline("<version>")`      | The released version that the compatibility check compares against, for example `"1.4.0"`. Default: the highest weaver release below `project.version`. |
| `compatibilityCheck = false`              | Turns the compatibility check off.                                                        |

Contracts of other modules and artifacts are not imported through `protoPath` but declared as weaver dependencies
(see [Imports from other modules](#imports-from-other-modules)). Overrides for the registry go into the `weaver { }`
block, not into `surfNats { }`.

## Contract registry

surf-nats distributes contracts through [weaver](https://weaver.slne.dev), the proto registry. The plugin applies
`dev.slne.weaver` and sets these conventions, so a module needs no `weaver { }` block:

| Setting                        | Default                                                       |
|--------------------------------|---------------------------------------------------------------|
| `weaver.url`                   | `WEAVER_URL` if set, else `https://weaver.slne.dev`           |
| `weaver.token`                 | `WEAVER_TOKEN`; optional for reading, required for publishing |
| `weaver.repositories`          | `proto-releases-public`, searched for dependencies            |
| `weaver.publishing.repository` | `proto-releases-public`, for releases and snapshots           |
| `weaver.publishing.group`      | `project.group`                                               |
| `weaver.publishing.name`       | `rootProject.name` for published modules, otherwise unset     |
| `weaver.publishing.version`    | `project.version`                                             |

Override any of them in `weaver { }`. A private repository needs the lookup and the publish target:

```kotlin
weaver {
    repositories("proto-private")
    publishing {
        repository = "proto-private"
        name = "profiles-contracts"
    }
}
```

The coordinates of a module are `group:name:version`. The plugin checks them against the weaver rules when a module
publishes, and fails the build if two modules of one build publish the same `group:name`. Set
`weaver.publishing.name` in one of them. A published release is immutable: publish a changed contract under a new
version.

### What the plugin does with the contracts

The plugin copies the `.proto` files of `surfNats.protoSourceDirs` into `build/surf-nats/weaver-source`
(`stageSurfNatsWeaverSource`) and publishes that directory. Weaver compiles the contracts when it receives them and
rejects a publish that does not compile.

| Task                         | Purpose                                                                                         |
|------------------------------|-------------------------------------------------------------------------------------------------|
| `stageSurfNatsWeaverSource`  | Copies the contracts into `build/surf-nats/weaver-source`.                                      |
| `fetchProtos`                | Downloads the weaver dependencies of the module into `build/weaver/protos`.                     |
| `fetchSurfNatsBaseline`      | Downloads the previous release of the module into `build/surf-nats/baseline`.                   |
| `checkSurfNatsCompatibility` | Compares the contracts with the baseline (see [Versioning](#versioning)).                       |
| `publishProtos`              | Uploads the contracts to weaver.                                                                |

The surf-nats protos (`surf/nats/v1/options.proto` and `surf/types/**`) come with the plugin, so code generation
works without network access. They are also published to weaver as `dev.slne.surf.nats:surf-nats-types-common` and
`dev.slne.surf.nats:surf-nats-types-paper`. The plugin adds `surf-nats-types-common`, at its own version, to the
publish manifest of every module that publishes, and `surf-nats-types-paper` as well in Paper modules
(`dev.slne.surf.api.gradle.paper-plugin` or `paper-raw`), so the server can resolve the imports. A release of your module cannot depend on a
snapshot of surf-nats: the server rejects that publish. Use a release of surf-nats for a release of your module.

### Publishing

A module publishes its contracts when it applies `maven-publish` and configures a Maven repository. Then `publish`
first runs `publishProtos`, which first runs `generateSurfNatsKotlin` and `checkSurfNatsCompatibility`. Every Maven
publish task runs after `publishProtos`, so a contract that weaver rejects stops the release before the Maven
upload. `publishProtos` also runs after the `publishProtos` of every `weaver(project(...))` dependency, because
weaver rejects a dependency it does not know yet. `publishToMavenLocal` does not publish contracts. A module without a Maven repository, such as a test
module, never publishes contracts and never needs the network.

`publishProtos` reads `WEAVER_TOKEN` and needs a token with write access to the repository.

### Imports from other modules

To import contracts from another module of the same build, or from an artifact in weaver, declare a weaver
dependency:

```kotlin
dependencies {
    weaver(project(":profiles-common"))
    weaver("dev.slne.surf.punish:surf-punish-contracts:1.2.0")
}
```

A project dependency must apply `dev.slne.weaver`; `dev.slne.surf.nats` applies it and stages `protoSourceDirs` as
`weaver.publishing.source`. The build uses that module's `weaver.publishing.source` directly, without publishing.
Publishing your module also needs a `weaver.publishing` group, name and version in the other module. A remote dependency needs an exact version; ranges and `+` are not supported. The dependencies
also appear in the manifest of your published artifact.

Imported files are import-only: the plugin does not generate Kotlin for them, and `fetchProtos` places them in
`build/weaver/protos`. A field of such a type fails with "neither generated nor mapped" until you map the type with
`mapType` to a class that your module can see. Copies of the surf-nats protos that arrive through weaver are ignored
in favor of the bundled ones.

`generateSurfNatsKotlin` and `checkSurfNatsCompatibility` depend on `fetchProtos` only when the module declares at
least one weaver dependency.

### Offline builds

A module without weaver dependencies builds with `--offline`. Fetching a remote dependency fails fast under
`--offline` and names the dependencies that need the network. Project dependencies work offline, unless the other
module itself depends on a remote artifact. Under `--offline`, `fetchSurfNatsBaseline` logs a warning and skips the
compatibility check.

## Clients

A `NatsClient` is your application's handle to NATS. Create one per plugin or microservice and close it when the
application shuts down:

```kotlin
val client = NatsClient.create("profiles")

// On shutdown:
client.close()
```

The client name identifies your application. It consists of lowercase letters and digits, separated by hyphens.
All instances of an application use the same name. Within one JVM, the name must be unique.

The name is not part of any address: the contract alone decides where events and calls travel (see
[Subjects](#subjects)). surf-nats uses the name to tell where a message came from (`Surf-Origin`), to name the
connection, and to group `ANY_INSTANCE` listeners per application.

`close()` closes every listener and service registration of the client. A closed client cannot be reused. After
surf-nats restarts, create a new client.

## Events

An event is a message with the option `(surf.nats.v1.event) = true`:

```proto
// surf/profiles/v1/name_changed.proto
message NameChanged {
  option (surf.nats.v1.event) = true;

  optional string player_id = 1;
  optional string name = 2;
}
```

The plugin generates `data class NameChanged(val playerId: String, val name: String)`. Publish an event with
`publish`:

```kotlin
client.publish(NameChanged(playerId = "alice", name = "Alice"))
```

`publish` does not suspend. It hands the event to the connection and returns. It does not wait for delivery, and
delivery is not guaranteed: an instance that is offline misses the event. An event must fit into a single NATS
message, whose size the server limits with `max_payload` (1 MB by default). For a larger event, `publish` throws an
`IllegalArgumentException`.

Only messages with the event option are events. Messages without it are ordinary types that events and services
use. Every application that listens to an event receives it, whichever client published it.

### Listening

A listener method is a method annotated with `@NatsListener`. It takes the event as its only parameter, returns
`Unit`, and may be `suspend`. You register the object that holds listener methods:

```kotlin
object NameListener {
    @NatsListener
    suspend fun onNameChanged(event: NameChanged) {
        println("${event.playerId} is now ${event.name}")
    }
}

client.registerListener(NameListener)
```

`registerListener` returns once the NATS server knows the subscription. Every event published after that reaches
the listener methods. `registerListener` returns a `NatsRegistration`. Close it to stop listening, or let
`client.close()` close it.

`registerListener` checks every listener method and reports all problems at once. Listener methods in superclasses
are registered too. A method that overrides a listener method must repeat `@NatsListener`.

A listener method that throws does not affect other listener methods. surf-nats logs the exception.

### Recipients

By default, every listening instance receives every event (`Recipients.ALL_INSTANCES`). This is the behavior of
surf-redis.

With `Recipients.ANY_INSTANCE`, each event goes to exactly one instance per client name, so every application that
listens still receives it once. Use it to spread work across the instances of your application:

```kotlin
@NatsListener(recipients = Recipients.ANY_INSTANCE)
fun onNameChanged(event: NameChanged) {
    // Runs on one instance only.
}
```

### Own events

A client receives its own events, as with surf-redis. To skip events that this client published in this JVM, set
`ignoreFromThisClient`:

```kotlin
@NatsListener(ignoreFromThisClient = true)
fun onNameChanged(event: NameChanged) {
}
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

`origin` has the form `<client name>@<instance id>`, where the instance id is a random id per JVM. It is `null` for
events from publishers that do not set it.

### Subjects

NATS delivers each message by its subject. surf-nats builds the subject of an event from the root `surf`, the kind
token `evt`, and the full name of the message in the contract, spelled as written there. The event `NameChanged` of
package `surf.profiles.v1` travels on `surf.evt.surf.profiles.v1.NameChanged`, whichever client publishes it. Every
application that listens to `NameChanged` subscribes to that subject: another plugin, a microservice, or an
application outside surf-nats such as a web panel.

Renaming the message or moving it to another package changes its subject (see [Names](#names)). The dots build a
hierarchy that subscribers can match with wildcards, such as `surf.evt.surf.profiles.v1.>` for every event of one
package.

## Services

A service is a `service` in a contract. Each `rpc` takes its own request message and returns its own response
message:

```proto
// surf/profiles/v1/profile_service.proto
service ProfileService {
  rpc FindName(FindNameRequest) returns (FindNameResponse);
  rpc Rename(RenameRequest) returns (RenameResponse);
}

message FindNameRequest {
  optional string player_id = 1;
}

message FindNameResponse {
  optional string name = 1 [(surf.nats.v1.nullable) = true];
}

message RenameRequest {
  optional string player_id = 1;
  optional string name = 2;
}

message RenameResponse {
  oneof result {
    string renamed_to = 1;
    string taken_by = 2;
  }
}
```

The plugin generates an interface with one `suspend` method per `rpc`. The method takes the request message as its
only parameter `request` and returns the response message:

```kotlin
interface ProfileService {
    suspend fun findName(request: FindNameRequest): FindNameResponse
    suspend fun rename(request: RenameRequest): RenameResponse
}
```

A request of type `google.protobuf.Empty` gives a method without parameters, and a response of type
`google.protobuf.Empty` a method that returns `Unit`; the file then imports `google/protobuf/empty.proto`. An empty
request message of your own, such as `message ListRequest {}`, stays a parameter (`list(request: ListRequest)`), and
it can gain fields later.

An instance registers an implementation, and any instance of any application calls it through a generated proxy.
A call to the `rpc` `FindName` travels on `surf.rpc.surf.profiles.v1.ProfileService.FindName`: the root `surf`, the
kind token `rpc` (`persist` for [persistent methods](#persistent-methods)), the full name of the service, and the
name of the `rpc`. All instances that register an implementation of a service share its calls, whichever client
registered them.

A call works like an event with a reply. The proxy publishes the call as a NATS message, an instance that listens
for it runs the method, and the result travels back as another message. Like events, NATS does not store these
messages. If no instance is running, the call reaches nobody, and if the instance stops while it runs the method,
the result never arrives. Depending on the method, the caller then gets an exception (see [Errors](#errors)) or
`Answer.Unknown`. [Persistent methods](#persistent-methods) store calls in JetStream instead.

Register the implementation on the instances that serve the calls:

```kotlin
object ProfileServiceImpl : ProfileService {
    override suspend fun findName(request: FindNameRequest) = FindNameResponse(repository.findName(request.playerId))
    override suspend fun rename(request: RenameRequest): RenameResponse = repository.rename(request.playerId, request.name)
}

client.registerService<ProfileService>(ProfileServiceImpl)
```

Call it from any instance, in this application or another one:

```kotlin
val profiles = client.service<ProfileService>()
val name = profiles.findName(FindNameRequest(playerId = "alice")).name
```

`service()` returns the same proxy every time for a given interface, so you can fetch it once and keep it.

### What a call does

What a call does depends on three things: whether the method is persistent (`persistent`, see
[Persistent methods](#persistent-methods)), which instances it addresses (`recipients`), and its response type.
`Answer<T>` is explained under [Asking all instances](#asking-all-instances).

| Persistent | Recipients      | Response                | Kotlin returns      | The call                                                                                                                       |
|------------|-----------------|-------------------------|---------------------|--------------------------------------------------------------------------------------------------------------------------------|
| no         | `ANY_INSTANCE`  | a message               | the message         | sends the call to one instance and waits for its result                                                                        |
| no         | `ANY_INSTANCE`  | `google.protobuf.Empty` | `Unit`              | sends the call to one instance and waits until it has run the method                                                           |
| no         | `ALL_INSTANCES` | `google.protobuf.Empty` | `Unit`              | sends the call to every instance and returns immediately, without waiting for any instance to run the method (fire and forget) |
| no         | `ALL_INSTANCES` | a message               | `Answer<message>`   | sends the call to every instance and returns the first known answer                                                            |
| yes        | `ANY_INSTANCE`  | `google.protobuf.Empty` | `Unit`              | returns once JetStream has stored the call, which one instance runs later                                                      |
| yes        | `ANY_INSTANCE`  | a message               | the message         | stores the call, which one instance runs, and waits for its result                                                             |

`ANY_INSTANCE` is the default. With `ANY_INSTANCE`, NATS picks one of the running instances and delivers the call
to that instance only. The other instances never see it, so only that instance runs the method. With
`ALL_INSTANCES`, every running instance receives the call and runs the method.

Persistent methods cannot use `ALL_INSTANCES`; the plugin rejects that combination. Changing the response type
changes what a call does.

The options live in the contract. Set them in the `(surf.nats.v1.service)` option to apply them to all methods,
and in the `(surf.nats.v1.method)` option of an `rpc` to override them for that method:

```proto
service SettingsService {
  option (surf.nats.v1.service) = { call_timeout_seconds: 10 };

  rpc SaveSettings(SaveSettingsRequest) returns (google.protobuf.Empty) {
    option (surf.nats.v1.method) = { persistent: true };
  }
  rpc ReloadSettings(ReloadSettingsRequest) returns (google.protobuf.Empty) {
    option (surf.nats.v1.method) = { recipients: NATS_RECIPIENTS_ALL_INSTANCES };
  }
}
```

A method option that is not set, a `call_timeout_seconds` of `0`, and `NATS_RECIPIENTS_UNSPECIFIED` inherit the
value of the service.

### Asking all instances

`ALL_INSTANCES` with a response message answers questions such as "on which server is this player?". The method
returns `Answer<T>`, and every instance answers with `Answer.Known(value)` or `Answer.Unknown`:

```proto
service PlayerService {
  rpc FindServer(FindServerRequest) returns (FindServerResponse) {
    option (surf.nats.v1.method) = { recipients: NATS_RECIPIENTS_ALL_INSTANCES };
  }
}

message FindServerRequest {
  optional string player_id = 1;
}

message FindServerResponse {
  optional string server = 1;
}
```

```kotlin
object PlayerServiceImpl : PlayerService {
    override suspend fun findServer(request: FindServerRequest): Answer<FindServerResponse> =
        if (isOnline(request.playerId)) Answer.Known(FindServerResponse(serverName)) else Answer.Unknown
}

val players = client.service<PlayerService>()
when (val answer = players.findServer(FindServerRequest(playerId = "alice"))) {
    is Answer.Known -> println("alice is on ${answer.value.server}")
    Answer.Unknown -> println("alice is offline")
}
```

The first `Known` answer ends the call. The call returns `Unknown` once every instance has answered `Unknown`, when
no instance serves the method, or when the timeout expires. If no instance answers `Known` and one of them threw,
the call throws that exception.

surf-nats tracks which instances are running, so a call can end as soon as all of them have answered. For about
one second after the first `service()` call and after a reconnect, it does not know the instances yet. A call in
that window that gets no `Known` answer waits for its timeout.

### Timeouts

A call waits 5 seconds by default, a persistent call 60 seconds. `call_timeout_seconds` on the service or on a
method changes the timeout. `withNatsCallTimeout` overrides it for every call inside the block:

```kotlin
val name = withNatsCallTimeout(500.milliseconds) {
    profiles.findName(FindNameRequest(playerId = "alice"))
}
```

A persistent call first waits for JetStream to confirm that it stored the call. While it waits, surf-nats sends the
call again whenever the confirmation does not arrive, each time with the same message id. JetStream discards a
message whose id it has already seen, but only within the last two minutes, the duplicate window that surf-nats
sets on its streams. A repeat sent later would be stored as a second call, so surf-nats waits for the confirmation
for at most two minutes.

For a persistent method without a result, the confirmation is all the call waits for, so its timeout can be at most
two minutes. The plugin rejects a longer `call_timeout_seconds` at build time, and the call rejects a longer
`withNatsCallTimeout` with an `IllegalArgumentException`. A persistent method with a result waits for the
confirmation for at most two minutes and then for the result until its full timeout, so its timeout can be longer,
for example five minutes.

### Errors

Calls throw `NatsRpcException` or one of its subclasses:

| Exception                   | Cause                                                                                                   |
|-----------------------------|---------------------------------------------------------------------------------------------------------|
| `NatsNoRespondersException` | No instance serves the method. Thrown at once, only for non-persistent `ANY_INSTANCE` calls.            |
| `NatsCallTimeoutException`  | No result or storage confirmation within the timeout. Never thrown by `ALL_INSTANCES` calls.            |
| `NatsRemoteException`       | The implementation threw. Carries the exception's class name and message, without stack trace or cause. |
| `NatsRpcException`          | The NATS server rejected a persistent call.                                                             |

A timeout does not mean that the method did not run. Design writes so that a retry is safe.

Expected outcomes belong in the response, not in exceptions. Model cases such as "name already taken" as a `oneof`
in the response message, like `RenameResponse` above, and reserve exceptions for bugs:

```kotlin
when (val result = profiles.rename(RenameRequest(playerId = "alice", name = "Alice")).result) {
    is RenameResponse.Result.RenamedTo -> println("renamed to ${result.value}")
    is RenameResponse.Result.TakenBy -> println("name belongs to ${result.value}")
    null -> error("no result")
}
```

A service method cannot return `null`. Use a response message with a `nullable` field instead, like
`FindNameResponse` above.

### Names

The names on the wire are the proto names from the contract: the full name of an event message, the full name of a
service, and the name of each `rpc`, spelled exactly as written. `java_package` and the names of the generated
Kotlin and TypeScript code play no part.

Field names do not travel on the wire; field numbers do. Renaming Kotlin code changes nothing, because the Kotlin
code is generated. Renaming a message, a service, or an `rpc` in the contract, or moving it to another package,
gives it a new address: callers that still use the old name reach nobody, and the compatibility check reports the
rename as a removal. Rename only in a new major version (see [Versioning](#versioning)).

## Versioning

The major version is part of the package (`surf.profiles.v1`) and therefore of every subject, stream, and service
registration. Within one major version, contracts must stay compatible:

- Compatible: new messages, services, and `rpc`s; new fields that are `nullable` or have a `[default]`, with a
  number above the current maximum; new enum values, which older readers decode as the zero value; removing a field
  that was `nullable` or had a `[default]`, if its number and name go into `reserved`.
- Incompatible: changing the number, type, or label of a field, even between wire-compatible types such as `int32`
  and `int64`; a new non-null field without a `[default]`; changing `[default]` or `nullable`; removing or renaming
  a message, enum, enum value, service, or `rpc` without reserving it; changing the zero value of an enum; changing
  the request or response type of an `rpc`; removing `(surf.nats.v1.event) = true` from an event; changing
  `persistent` or `recipients` of a method; moving a type to another Kotlin package; removing a reservation.

For an incompatible change, create a new major version: a package `surf.profiles.v2` in `surf/profiles/v2/`, with
`java_package` ending in `.v2`. Versions 1 and 2 have their own subjects, streams, and registrations, and one client
can register the implementations of both. Roll out in this order: let the implementations serve both versions,
move the callers to version 2, then remove version 1.

The task `checkSurfNatsCompatibility` runs with `check` and compares the contracts against the last release of the
module in weaver, the baseline. `fetchSurfNatsBaseline` picks the highest release below `project.version` in
`weaver.publishing.repository` and downloads its contracts into `build/surf-nats/baseline`. To compare against a
specific release instead, set its version:

```kotlin
surfNats {
    compatibilityBaseline("1.4.0")
}
```

The check runs for modules that publish to a Maven repository, and for modules that set a baseline. It is skipped
when the module has no release in weaver yet, and under `--offline` with a warning. It writes its report to
`build/reports/surf-nats/compatibility.txt`. A new field with a number below the current maximum is wire-compatible but shifts the Kotlin parameters, so the check
reports it as a warning. The release that removes version 1 reports every element of version 1 as removed and needs
`compatibilityCheck = false`.

## Persistent methods

A persistent method stores each call in JetStream before an instance runs it. The call survives restarts: if no
instance is running, it waits until one starts. Use persistent methods for writes that must not get lost. Keep reads
non-persistent, so that they fail fast when the service is down.

```proto
service ProfileService {
  rpc Rename(RenameRequest) returns (google.protobuf.Empty) {
    option (surf.nats.v1.method) = { persistent: true };
  }
  rpc FindName(FindNameRequest) returns (FindNameResponse);
}
```

To make all methods persistent except a few, set `persistent: true` in the service option and `persistent: false`
in the method option of the exceptions.

All instances of a service read its persistent calls through one shared consumer, which delivers calls of every
persistent `rpc` of the service. An instance that does not know an `rpc`, such as one still running an older
release, fails each call to it and the call is retried. A call without a result finally moves to the dead-letter
stream (see [Calls without a result](#calls-without-a-result)). A call with a result expires when its timeout ends,
unless the retries end first, and its caller fails with a timeout. Roll out a new persistent `rpc` to every instance
that serves the service before any caller uses it.

### Calls without a result

A persistent method whose response is `google.protobuf.Empty` returns as soon as JetStream has stored the call. One
instance runs it later. If the implementation throws, surf-nats retries after 1 second, 5 seconds, 30 seconds, and 2
minutes. After the fifth failed attempt (the first attempt and four retries), surf-nats moves the call to the
service's dead-letter stream, where it waits for manual inspection (see [Operations](#operations)). A call that
cannot be decoded goes to the dead-letter stream at once.

While the implementation runs, surf-nats tells JetStream every 5 seconds that the call is still in progress.
Without these signals, JetStream would assume after 10 seconds that the instance had crashed and deliver the call
again. The signals have a downside: a method that never returns would hold its call forever, and JetStream would
neither retry it nor move it to the dead-letter stream. surf-nats therefore cancels the method after 10 minutes and
counts the attempt as failed, so the call is retried or dead-lettered like any other failure.

The cancellation is a coroutine cancellation, which stops the method only at its next suspension point. Blocking
code, such as a JDBC query, does not notice it and keeps running, and the signals to JetStream continue with it.
Wrap blocking code in `runInterruptible(Dispatchers.IO)`, which interrupts the thread when the method is cancelled.

### Calls with a result

A persistent method that returns a message stores the call and waits for the result. If the implementation throws,
or the call cannot be decoded, the caller receives a `NatsRemoteException`, and surf-nats does not retry.

### Running a call more than once

JetStream delivers each call at least once. If an instance crashes before it confirms a call, JetStream delivers
the call again, so the implementation must tolerate running twice. `natsPersistentContext()` tells the
implementation which delivery it handles:

```kotlin
override suspend fun rename(request: RenameRequest) {
    val context = natsPersistentContext()
    println("message ${context.messageId}, attempt ${context.deliveryCount}")
}
```

`messageId` stays the same across retries of one call. Two calls with the same request get different ids, so
`messageId` cannot detect an operation that a caller sends twice. Use a key from your domain for that, such as an
operation id that the caller generates and passes as a field of the request.

### Deadlines

Each persistent call with a result carries a deadline: the time of the call plus its timeout. An instance drops
calls whose deadline has passed, because the caller no longer waits for the result. The deadline is an absolute
time, so all hosts need synchronized clocks (NTP).

### Streams

`registerService` creates the JetStream stream and consumer of a persistent service. A Kotlin caller also creates
the stream on its first call, so calls are stored even before any instance has registered. A caller outside the
JVM cannot do that: its calls fail until the service has been registered once.

## Large payloads

The NATS server limits the size of a message with `max_payload`, 1 MB by default. When the request or the result
of a service call exceeds that limit, surf-nats stores it in the NATS object store and sends a reference instead.
The receiver loads it transparently. surf-nats deletes the objects after use, and objects it does not delete
expire after the time in the table below. Your code passes ordinary messages, for example a request with a
`bytes data` field:

```kotlin
suspend fun upload(request: UploadRequest)
```

| Bucket                          | Holds                                                                            | Kept for   |
|---------------------------------|----------------------------------------------------------------------------------|------------|
| `surf-nats-payloads`            | requests and results of non-persistent calls and persistent calls with a result  | 10 minutes |
| `surf-nats-persistent-payloads` | requests of persistent calls without a result                                    | 7 days     |

The object store needs JetStream and a `max_payload` of at least 128 KB. Events never use it.

Objects in `surf-nats-payloads` expire after 10 minutes, whatever the timeout of the call. A call with a longer
timeout therefore fails when its request or result needs the object store: a request with an
`IllegalArgumentException`, a result with a `NatsRemoteException`. Calls with small payloads are not affected.

## Threads

surf-nats runs listeners and service implementations concurrently on `Dispatchers.Default`, in no particular order.
Do not block there. Move blocking work to `Dispatchers.IO`:

```kotlin
override suspend fun findName(request: FindNameRequest) = withContext(Dispatchers.IO) {
    FindNameResponse(database.findName(request.playerId))
}
```

Listener methods and service implementations do not run on the server's main thread. Use the platform scheduler for
Paper, Velocity, or Minestom APIs that must run on the main thread.

`NatsRegistration.close()` and `NatsClient.close()` block the calling thread while services finish their running
calls, for up to `drain-timeout-seconds`. Running listener methods are not waited for.

## Operations

### Streams and dead letters

Each persistent service has a stream for its calls and a dead-letter stream for calls that failed for good. Their
names are `SURF_PERSIST_<service>` and `SURF_DLQ_<service>`, where `<service>` is the full name of the service with
every `_` doubled and then every `.` replaced by `_`, in its original case:

| Service                           | Stream                                          | Dead-letter stream                          |
|-----------------------------------|-------------------------------------------------|---------------------------------------------|
| `surf.profiles.v1.ProfileService` | `SURF_PERSIST_surf_profiles_v1_ProfileService`  | `SURF_DLQ_surf_profiles_v1_ProfileService`  |
| `surf.player_data.v2.DataService` | `SURF_PERSIST_surf_player__data_v2_DataService` | `SURF_DLQ_surf_player__data_v2_DataService` |

The stream stores the subjects `surf.persist.<service full name>.*`, the dead-letter stream the subject
`surf.dlq.<service full name>`. All instances of the service share the durable consumer `responders` on the stream.
A stream name may have at most 255 characters; the plugin rejects longer service names.

Both streams keep messages for 7 days. surf-nats does not process the dead-letter stream. Inspect it with the
`nats` CLI, fix the cause, and publish the calls to the service again:

```sh
nats stream view SURF_DLQ_surf_profiles_v1_ProfileService
```

[External callers](docs/external-callers.md#dead-letters) describes the headers of a dead letter and how to
publish the call again.

### Known limits

- A call can stay in the stream without further attempts until it expires after 7 days. This happens when moving it
  to the dead-letter stream fails, or when the instance crashes during the last attempt.
- A call within about 150 bytes of `max_payload` does not fit into the dead-letter stream, because surf-nats adds
  headers to it. It stays in the stream until it expires.
- A dead-lettered call whose arguments live in the object store can only be published again within 7 days of the
  original call.
- If a stream is deleted, restart every instance that registers or calls the service. surf-nats does not recreate
  deleted streams or consumers while running.
- A new major version, or a renamed service, uses a new stream. The old stream and its unprocessed calls stay
  behind.
- Instances that share a consumer use the consumer settings of the instance that registered last. Keep versions
  aligned during rolling updates.
- Repeated warnings about expired calls can point to clock skew between hosts (see [Deadlines](#deadlines)).

### Upgrading from earlier snapshots

Snapshots before the current address scheme put the client name into subjects and named their streams
`SURF_<CLIENT>_<SERVICE>_V<N>`, `SURF__<SERVICE>_V<N>`, and the same with `_DLQ`. surf-nats neither reads nor
migrates them. Process or drop the calls they still hold, then delete these streams and their durable consumers,
for example with `nats stream rm <stream>`, which removes their durable consumers as well.

## External callers

Applications outside the JVM, such as a web panel, can publish and receive events and call services. They read the
contracts from weaver and send Protocol Buffers messages with the header `Content-Type: application/protobuf`. They
need no client name of the application that serves a contract: the contract alone determines the subjects. A
persistent method without a result fits commands that need no answer. Register a persistent service once before the
external application publishes calls.

A Node.js project fetches the contracts with the weaver CLI `@slne/weaver-cli` (`weaver fetch`) and generates code
from them with `buf generate`. For Node.js and TypeScript, `surf-nats-core` builds the client `@slne/surf-nats`,
which works with [protobuf-es](https://github.com/bufbuild/protobuf-es) schemas, and its protoc plugin
`protoc-gen-surf-nats`, which generates a typed client per service next to the protobuf-es code.
[External callers](docs/external-callers.md) describes the setup, with `weaver.json` and `buf.gen.yaml` examples,
and the wire format for other NATS clients: subjects, headers, replies, and dead letters.

## Migrating from surf-redis and surf-rabbitmq

| surf-redis / surf-rabbitmq             | surf-nats                                                                                                                                  |
|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `@Serializable class X : RedisEvent()` | A message with the `(surf.nats.v1.event)` option in a contract                                                                             |
| `@OnRedisEvent`                        | `@NatsListener`                                                                                                                            |
| `subscribeToEvents(listener)`          | `client.registerListener(listener)`                                                                                                        |
| `publishEvent(event).await()`          | `client.publish(event)`                                                                                                                    |
| Request and response packet            | An `rpc` with its own request and response message. The generated method takes the request and returns the response.                       |
| `@RabbitHandler`                       | The implementation of that method                                                                                                          |
| `sendRequest(packet)`                  | A call on `client.service<T>()`                                                                                                            |

Points that behave differently:

- **Requests are no longer persistent by default.** surf-rabbitmq stored requests in a durable queue for up to 60
  seconds. A non-persistent surf-nats call fails at once with `NatsNoRespondersException` when no instance runs.
  Mark methods with side effects `persistent: true`. Keep reads non-persistent.
- **One `rpc` per request type.** Replace packets that switch on a type field with one `rpc` per type, each with its
  own response message.
- **Subjects come from the contract.** An event travels on `surf.evt.<message full name>`, a call on
  `surf.rpc.<service full name>.<rpc>`. Renaming a message, a service, or an `rpc` changes its subject.
- **Commands that only some instances can handle become services.** A message such as "send this player a message",
  which only the server the player is on can carry out, belongs in an `rpc` with `ALL_INSTANCES`: a
  `google.protobuf.Empty` response to send it to every instance, or a response message to learn which instance
  handled it. Every instance receives the call and decides whether it is responsible.
- **Exceptions arrive as `NatsRemoteException`.** Return expected failures as a `oneof` in the response.
- **Messages are Protocol Buffers.** Applications that published JSON or the surf-redis format must send Protocol
  Buffers messages with `Content-Type: application/protobuf`.

## Modules

| Module                    | Contents                                                                                       |
|---------------------------|------------------------------------------------------------------------------------------------|
| `surf-nats-gradle-plugin` | The Gradle plugin `dev.slne.surf.nats`: code generation, build checks, compatibility check, weaver integration |
| `surf-nats-api`           | The public API: client, `@NatsListener`, contexts, `Answer`, exceptions, generated-code model  |
| `surf-nats-core`          | The implementation for the JVM, and the Node.js client `@slne/surf-nats`                       |
| `surf-nats-types-common`  | `surf/nats/v1/options.proto`, the `surf.types.v1` messages, and their Kotlin serializers; the protos are also published to weaver |
| `surf-nats-types-paper`   | The `surf.types.paper.v1` messages and their Paper serializers; the protos are also published to weaver |
| `surf-nats-paper`         | The Paper plugin                                                                               |
| `surf-nats-velocity`      | The Velocity plugin                                                                            |
| `surf-nats-minestom`      | The Minestom plugin                                                                            |
| `surf-nats-standalone`    | The runtime for microservices, started by surf-microservice                                    |
