# Spring migration decisions

This document records how surf-api 4.0 wires its beans with Spring and why each part works the way
it does. It is written for maintainers of surf-api and of the infra libraries under `infra/`. For
the changes that downstream plugin authors must make, see `MIGRATION-4.0.md`.

surf-api 4.0 has no compatibility layer for the dependency-injection API of 3.x.

## Scope

The migration covers:

- every platform: Paper, Velocity, Minestom and the standalone runtime
- the infra libraries: NATS, Redis, the microservice runtime and the database library
- the Gradle plugins that build surf plugins and microservices

It does not cover replacing Configurate, the `SurfEventBus` or CommandAPI.

## Application contexts

Spring Boot runs only in microservices, where one application owns the process and its context is
surf-api's root context. Every other context is a plain `AnnotationConfigApplicationContext`: the
root context on Paper, Velocity, Minestom and the standalone runtime, and every plugin context. The
platforms ship Spring Framework but not Spring Boot, so `@EnableAutoConfiguration` and
`@ImportAutoConfiguration` are available only in microservices. A standalone program that wants
Spring Boot hands its context to surf-api with `SurfApiStandaloneBootstrap.adopt`, as the
microservice runtime does.

Each JVM has three kinds of context:

Root context
: One per JVM. Holds surf-api's own beans, such as the event bus, the config API and the platform
  bridges.

Import context
: One per plugin. Its parent is the root context. It holds only the beans that the plugin's
  **direct** dependencies export.

Plugin context
: One per plugin. Its parent is the plugin's import context. Holds every bean of the plugin.

On Paper the root is split the same way the plugins are: a root bootstrap context, built in
surf-api's bootstrap phase with surf-api's bootstrap components, and the root context, built in
surf-api's load phase below it with every other surf-api bean. surf-api ships no bootstrap
components, so every surf-api bean is created in the load phase, when the Bukkit server API is
available. A plugin's bootstrap context sits below the root bootstrap context; its import context
sits below the root context and also references the plugin's own bootstrap beans, so the plugin's
full context can inject them. See [Lifecycle](#lifecycle).

surf-api's static entry points read their beans lazily from the installed root context through
`surfBean`, which keeps the bean of the first root it read. `SurfEventBus.INSTANCE` resolves the bus
again after a new root context is installed, so `call()` and the automatically registered
`@SurfEventHandler` beans use the bus that the current root's beans inject. The entry points are not
part of the public API: plugins inject a service or call the extension and top-level functions that
wrap it. An accessor used only in its own module is `internal`; one that other surf-api modules
use is public but carries `@InternalSurfApi`, whose error-level opt-in keeps plugins from compiling
against it. The root build opts every surf-api module into
`@InternalSurfApi`; the Gradle plugin that downstream projects apply does not.

The root contexts scan `dev.slne.surf.api` with surf-api's own class loader. A plugin with a class
loader of its own is invisible to that scan, whatever its package. A plugin that shares surf-api's
class loader, such as one shaded into a Minestom server, must live outside `dev.slne.surf.api`, and
its base package must not contain it either; otherwise the plugin's beans would land in the root
context or surf-api's beans in the plugin context. `SurfContextManager` rejects such a plugin.

Platform objects that surf-api's beans need are singletons of the root context: the Minestom
server is injectable as `SurfMinestomServer`. The inventory framework's `ViewFrame` is a root bean
on Paper and Minestom, next to a `ViewFrameAccessor` bean that wraps it; a plugin injects either.

### Exports

A plugin exports a bean by implementing an interface annotated with `@PluginApi`. The interface
lives in the plugin's api module; the implementation is an ordinary `@Service`. Everything else,
such as repositories, configuration and internal helpers, stays private to the plugin context.

Exports follow these rules:

- An export is registered in the consumer's import context as a lazy, synthetic definition carrying
  its full generic type, primary, fallback and default-candidate flags and qualifier. On the first
  real lookup the import factory's `createBean` returns the provider's instance instead of creating
  one, and Spring caches it as a singleton. The import context therefore never initializes,
  post-processes or destroys it; its owner manages its lifecycle. Type checks never fetch it.
  `FactoryBean` delivery was rejected: in Spring's fallback match pass it erases a declared generic
  type such as `Repo<Foo>`, which would let the export satisfy a `Repo<Bar>` dependency.
- An export that also implements Spring's `Lifecycle` or `ApplicationListener` is rejected at
  startup, because the consumer context would otherwise start, stop or register it a second time.
- `@Qualifier` and `@Primary` travel on the export's definition.
- Two different exports under one import name fail the consumer's start; a dependency listed under
  its name and an alias is imported once.
- Only direct dependencies are visible. A plugin re-exports a transitive dependency only explicitly.
- If two dependencies export the same type, a single-valued injection fails the consumer's start,
  also when it is optional (`T?`, `Optional<T>`, `ObjectProvider.getIfAvailable`), and a
  programmatic lookup by type finds no unique bean; only `ObjectProvider.getIfUnique` returns
  `null`. The consumer selects one with `@Qualifier`. Neither load order nor a provider's `@Primary`
  or `@Priority` decides.
- A provider that is disabled while plugins importing from it are still open keeps its contexts
  open until the last of them closes, so no consumer ever holds a destroyed bean.
- Only a plain `@Qualifier` value travels with an export; custom qualifier annotations do not.
  `@Fallback` and default-candidate flags do.

### Soft dependencies

A soft dependency is used only through an adapter bean annotated with `@ConditionalOnPlugin`.
Spring evaluates the condition from bytecode metadata without loading the class, so the adapter's
reference to the dependency's API never triggers a `NoClassDefFoundError` when the dependency is
absent. The rest of the plugin injects the adapter's interface as a nullable type.

One `@ConditionalOnPlugin` stands for one plugin and holds when any of its names is installed, so
code shared by several platforms can list a plugin's per-platform names. The annotation is
repeatable; every repetition must hold.

The plugin named in `@ConditionalOnPlugin` must be a declared dependency, optional for a soft one.
On Velocity the condition is evaluated in the plugin's constructor, and Velocity only knows the
plugins it constructed before.

## Plugin integration

The Gradle plugin generates the platform main class for Paper, Velocity and Minestom, and on Paper
also the bootstrapper: `GeneratedSurfPlugin` and `GeneratedSurfBootstrapper` in the plugin's base
package. A plugin that needs its own class names it in the Gradle plugin (`mainClass`,
`bootstrapper`, `velocityPluginFile.main`, `minestomPluginFile.main`). The task
`checkSurfPluginClasses`, run by `check` and before every jar, verifies from the class files that a
named class extends `SurfPaperPlugin`, `SurfPaperBootstrapper`, `SurfVelocityPlugin` or
`SurfMinestomPlugin`; the lifecycle methods of those classes are final, so they cannot be
overridden. `verifyPluginClasses = false` turns the check off for a plugin that is not a surf
plugin yet.

On Paper, `surfPluginDependency("name")` declares another surf plugin as both bootstrap and server
dependency, required, with joined class path and loaded before. Other plugins stay plain server
dependencies: a required bootstrap dependency on a plugin without a bootstrapper makes Paper skip
the dependent plugin. CommandAPI is always added as a server dependency with joined class path,
because Paper does not pass its classes on through surf-api.

The plugin instance is the primary bean of the plugin context and can be injected by its concrete
type and by every supertype. The platform creates the instance, but the context manages it like any
other singleton: it injects `@Autowired` members, runs the `Aware` callbacks, `@PostConstruct` and
every `BeanPostProcessor` on it, and calls `@PreDestroy` and `DisposableBean.destroy` when it
closes. Only Spring's inferred `close()` or `shutdown()` call is suppressed, because the platform
owns the instance. Cleanup belongs in either `onDisable` or `@PreDestroy`, not both, or it runs
twice. The surf-api plugin itself is not a bean of the root context, so `Plugin` inside a plugin
context resolves to that plugin's own instance.

Component scanning starts at the longest common package of the plugin's own sources and reads only
the plugin's own jar. The Gradle plugin computes that package from the `package` declarations of
the main sources; `basePackage` overrides it. It rejects an empty package and every package that
contains surf-api's own, such as `dev.slne.surf`, and on Minestom also packages inside it, because a
Minestom plugin can share surf-api's class loader. The generated classes sit in that package, which
is how the runtime learns it. With a named class instead, the runtime scans that class's package and
`basePackage` has no effect; the build warns if the class's package misses part of the sources.
`generatePluginClass = false` turns generation off for a module that is not a plugin itself.

The task `checkNoBundledSpring`, run by `check` and before every shadow jar task, fails a Paper,
Velocity or Minestom plugin whose shadow jar would contain Spring, which ships inside surf-api. It
reads the configurations the shadow jar bundles and fails on a module of an `org.springframework`
group and on any bundled jar or zip with unrelocated `org/springframework/` entries, at the root or
under `META-INF/versions/<n>/`, so a fat jar that carries Spring under another coordinate is
caught too. It runs only while surf-api is `compileOnly`, because a plugin that bundles surf-api
also bundles its Spring. The plain `jar` task bundles nothing and does not wait for it.
`allowBundledSpring = true` turns it off; standalone applications, Minestom servers and
microservices never run it.

## Lifecycle

`SurfLifecycle` gives beans the suspending hooks `onLoad`, `onEnable` and `onDisable`;
`SurfBootstrapLifecycle` adds `onBootstrap` on Paper. The bootstrap hook is an interface of its own
because implementing it decides when Paper creates the bean (see below), and Kotlin compiles a
forwarding method for every interface default into each implementing class, so an optional hook
inside `SurfLifecycle` could not be told apart from an overridden one. Within a context, the
hooks run in the order in which the beans finished initializing, which is a valid dependency order,
and `onDisable` runs in reverse; `@Order` moves a bean ahead of the unordered ones. Constructors and
`@PostConstruct` only wire objects; they must not block or need loaded worlds.

surf-api drives each plugin context through the platform's phases:

| Platform | Refresh | `onLoad` | `onEnable` | `onDisable` and close |
|---|---|---|---|---|
| Paper | plugin bootstrapper, followed by `onBootstrap` | plugin `onLoad` | plugin `onEnable` | plugin `onDisable` |
| Velocity | plugin constructor | plugin constructor | `ProxyInitializeEvent` | `ProxyShutdownEvent` |
| Minestom | plugin manager load | plugin manager load | plugin manager enable | teardown |
| Standalone | `bootstrap` (root only) | `bootstrap` | `enable` | `shutdown` |

On Paper a plugin's first context is built in the bootstrap phase, because plugins such as
surf-core already do work there. The Gradle plugin generates a bootstrapper unless the plugin names
its own, and declares every surf plugin listed with `surfPluginDependency` as a bootstrap and
server dependency, so the contexts of those dependencies exist when the plugin bootstraps; surf-api
builds its root bootstrap
context in its own bootstrapper and its root context in its own load phase. Paper's
`BootstrapContext` is injectable during the bootstrap phase.

In the bootstrap phase a plugin sees only the classes of its bootstrap dependencies, so the APIs of
Bukkit plugins such as LuckPerms or PlaceholderAPI are out of reach, and Paper has not created the
plugin instance yet. Refreshing the plugin's whole context then would fail: Spring loads and
inspects every bean class during a refresh, even one it creates later, so a bean extending
`PlaceholderExpansion` or a `@Bean` method returning `LuckPerms` would break the bootstrap. A Paper
plugin therefore has two contexts:

Bootstrap context
: Built in the bootstrap phase below the root bootstrap context. It holds only the plugin's
  components that implement `SurfBootstrapLifecycle` or carry `@BootstrapComponent`, found by
  reading bytecode metadata without loading a class. Their dependencies must be such components
  themselves or the beans the bootstrap context registers: Paper's `BootstrapContext` and the
  `Plugin` proxy.

Plugin context
: Built in the load phase, below the plugin's import context, which sits below the root context
  and references the bootstrap beans. It holds every other component, can inject the bootstrap
  beans, and evaluates
  `@ConditionalOnPlugin` against Bukkit's plugin manager.

A bootstrap bean must not depend on a non-bootstrap plugin's API, must not use the Bukkit server
API and must not dispatch to the main or region thread, which does not tick yet. The hooks of the
bootstrap context's beans run before those of the plugin context, and `onDisable` in reverse.

The plugin instance is injectable as `org.bukkit.plugin.Plugin` in the bootstrap context as a proxy
that forwards to the instance once Paper created it; it compares equal to the instance, the way
Bukkit compares plugins, so listeners and tasks registered through it are cleaned up with the
plugin. The plugin context injects the instance itself under every type it has.

The plugin's hooks block the thread Paper calls the phase on, which is the main thread for load,
enable and disable, so a hook must not wait for work dispatched to the main or a region thread.
A plugin whose load phase failed is never enabled; Paper disables it instead.

There is no hot reload or unloading. At shutdown, contexts close in reverse dependency order:
consumers and their import contexts first, then providers, then the root context.

## Coroutines

surf-api does not use mccoroutine. Plugin scopes, the Paper dispatchers and the entity context are
surf-api code; see [Rejected alternatives](#rejected-alternatives).

### Scopes

Every plugin context and surf-api's root context hold a `CoroutineScope` bean, built on every
platform by `surfCoroutineScope(owner, ...)`. The owner is the plugin's name on Paper, its id on
Velocity and Minestom, and `surf-api` for the root. The scope has:

- a `SupervisorJob`
- the coroutine name `surf-<owner>`
- a handler that logs uncaught failures through the commons-logging logger `surf-coroutines` as
  `Unhandled exception in a coroutine of '<owner>'`
- the plugin's class loader as thread context class loader; a Minestom plugin without a jar of its
  own gets the server class loader
- `Dispatchers.Default` as default dispatcher

The plugin's scope is primary, so a plugin bean that injects `CoroutineScope` gets its own, not the
root's. Lifecycle hooks run with the scope's elements, without its job, dispatcher and exception
handler.

The thread context class loader travels as `ContextClassLoaderElement`, and blocking code switches
it with `withContextClassLoader`. Both live in `surf-api-spring-impl`
(`dev.slne.surf.api.spring.context`) and are public but `@InternalSurfApi`, so the platform
runtimes and the Redis and NATS integrations share one implementation.

The plugin context owns the scope: it cancels it once the disable hooks have run, and again when its
contexts close, even if the close is deferred because other plugins still import from it. A context
that cannot be built cancels the scope it was given. Paper's bootstrap contexts have no scope.

The default dispatcher is `Dispatchers.Default`, not the server thread. Paper refuses to run a
plugin's tasks before it is enabled and after it is disabled, so a scope that defaults to the
server thread would stall or cancel every coroutine that resumes during load or disable. Code that
has to run on the server thread asks for it explicitly. Suspending command executors, requirements
and arguments start on the server thread: their default scope is the declaring plugin's scope with
its main dispatcher.

### Paper dispatchers

Every plugin context and the root context hold a `PaperDispatchers` bean with `main` (the server
thread, on Folia the global region), `region(...)`, `entity(entity)`, and `async`, which is
`Dispatchers.Default`. The dispatchers admit work by the plugin's lifecycle rather than by Paper's
scheduler, which throws or silently drops the tasks of a plugin that is not enabled:

- Before the plugin is enabled, work is parked and handed to Paper when it is.
- While a lifecycle phase of any plugin blocks the server thread, the main work of every plugin's
  dispatchers runs inside that phase's event loop, so a hook can wait for it, even for work
  surf-api or another plugin dispatched. On Paper, where the server thread also runs every region
  and entity, that includes region and entity work. On Folia the phase's thread counts as the main
  thread, because the global region does not tick yet while plugins load and enable; a hook there
  must not wait for region or entity work.
- When the plugin starts to disable, everything Paper still holds is released before the hooks run,
  and later work is rejected.

A rejected block is never dropped. The dispatcher cancels the coroutine and runs the block on
`Dispatchers.IO`, where the cancellation surfaces at the suspension point; the same happens when an
entity is removed before its work ran. Each block runs or is rejected exactly once. The
cancellation surfaces even inside `withContext(NonCancellable)`, because it cancels that block's
own job; `finally` blocks run on the IO thread and must not touch the entity or the world, and
cleanup that has to finish after the plugin is disabled stays on `Dispatchers.IO`.

`entity.withEntity { }` takes a block that does not suspend, like Minestom's. If the entity is
removed or the plugin disabled before the block runs, the block never runs and the call fails with a
`CancellationException`; it is the safe way to touch an entity. `Int.ticks` is the nominal duration
of that many ticks, 50 ms each.

Code that is not a bean, such as the API's extension functions, finds a plugin's scope and
dispatchers through `SurfPaperPluginRuntime.coroutineScope(plugin)` and `dispatchers(plugin)`.

### Listeners and commands

Paper gets no bridge for suspending listeners. A Bukkit event is synchronous, so a suspending
listener could not reliably cancel it or know which thread it resumes on; Paper listeners launch
their own coroutines instead. Velocity supports suspending `@Subscribe` handlers, because
Velocity's event system is asynchronous by design and lets a handler finish later. Commands use
surf's own suspending executors.

The executor DSL's default scope is looked up by a function that downstream code inlines, so a
plugin compiled against surf-api 3.x has to be recompiled.

### Minestom

Minestom uses its own dispatchers. The plugin manager creates each plugin's scope while the plugin
is constructed, so property initializers can use it, and hands it to the plugin context, which owns
it from then on.
`entity.withEntity { }` cancels instead of waiting forever when the entity is removed before the
block ran.

## Default configuration of plugin contexts

Spring does not inherit bean post-processors from a parent context, so surf-api sets up every
plugin context on its own. Each context gets the lifecycle hook runner, a bean post-processor that runs
the hooks of the `SurfLifecycle` and `SurfBootstrapLifecycle` beans. A plugin's full context then
receives, in this order:

1. The `PluginPresence` that `@ConditionalOnPlugin` evaluates.
2. The plugin's components, found by the component scan.
3. The plugin's `CoroutineScope`, as primary bean.
4. The contributions of every `ContextContributor` bean in the root context.
5. The platform's beans: the plugin instance, as primary bean, on every platform; the plugin's
   `PaperDispatchers`, as primary bean, on Paper; `ProxyServer` and the plugin's
   `PluginContainer` on Velocity.

surf-api ships three contributors in its Paper and Velocity plugin jars and its Minestom runtime:
`SurfConfigContributor` registers a `ConfigHandle` for every `@SurfConfig` class,
`NatsContextContributor` the plugin's NATS client and the NATS post-processors, and
`RedisContextContributor` the plugin's `RedisApi`. Bootstrap contexts get no contributions,
because the root context does not exist yet in Paper's bootstrap phase.

Every `PluginDescriptor` carries the plugin's data folder: Paper's `BootstrapContext.dataDirectory`,
`plugins/<id>` next to surf-api's own folder on Velocity, and the plugin manager's folder on
Minestom.

## Configuration

Configuration uses Configurate. Spring's `@ConfigurationProperties` only binds values; it cannot
write defaults with comments, save edits made in game, run versioned migrations or apply the
constraint annotations.

A config class carries `@ConfigSerializable` and `@SurfConfig`:

```kotlin
@ConfigSerializable
@SurfConfig("data/shop.yml", migrations = [ShopV1::class, ShopV2::class])
data class ShopConfig(val discount: Double = 0.0)
```

- `@SurfConfig` also works as a meta-annotation: a custom annotation that carries
  `@SurfConfig(path)` marks a config class the same way.
- The path is relative to the plugin's data folder, may contain subfolders and must not leave it.
  Its extension picks the format: `.yml` or `.yaml` for YAML, `.json` for JSON.
- A migration's version is its position in `migrations`, starting at 1, so a migration is only
  ever appended. Each one is a Kotlin `object` or has a constructor without parameters.
- Beans inject `ConfigHandle<ShopConfig>`, which offers `value`, `reload()`, `edit { }` for classes
  with mutable properties, `update { it.copy(...) }` for immutable ones, and `save()`. Injecting the
  data class directly would hand out a stale value after a reload. The handle serializes its
  operations, because Configurate's loader and node are not thread-safe.
- The config contributor finds `@SurfConfig` classes in the plugin's own jar, the same way the
  component scan does, and registers one handle per class with the class as its exact type
  argument. The class itself is no bean.
- The file is read, migrated and written back with its defaults when the plugin's context starts,
  so a broken file fails the plugin's start instead of its first use.
- Handles do not go through `SurfConfigApi`'s global registry, which would keep a plugin's classes
  alive after the plugin is gone.
- A plugin registers serializers for its own types through `ConfigSerializerRegistrar` beans, which
  run before its first handle opens; the static accessor to the serializer registry is internal.

Microservices use the same mechanism for their own configuration and for the database and NATS
files.

## Events, listeners and commands

The `SurfEventBus` carries events between plugins; Spring events only travel from a
context to its ancestors.

In the enable phase, surf-api registers every listener bean of a plugin context and unregisters it
on disable:

- Paper: beans implementing `Listener`
- Velocity: beans with `@Subscribe` methods
- Minestom: beans of type `EventNode`, attached to the plugin's event node
- every platform: beans with `@SurfEventHandler` methods

On Velocity the listener beans are registered while Velocity fires `ProxyInitializeEvent`, so their
handlers for that event never run; a bean that has to act once the proxy is up uses
`SurfLifecycle.onEnable`. surf-api's own handlers for `ProxyInitializeEvent` and
`ProxyShutdownEvent` run before and after every other handler, so a plugin's handlers for those
events find its beans enabled.

`SurfEventHandlerBeans` in the core implementation registers the event bus listeners on every
platform. The standalone runtime has no plugin contexts, so `SurfApiStandaloneBootstrap.enable`
registers the beans of the root context, which hosts the application, before its `onEnable` hooks,
and `shutdown` unregisters them after the `onDisable` hooks. Microservices adopt their context
through `SurfApiStandaloneBootstrap.adopt` and take the same path. `CommandApiCommandBeans`
registers the commands on Paper and Velocity, which run CommandAPI; Minestom registers its own
`CommandAPICommand` and `CommandTree` beans.

surf-api registers commands in the enable phase, before the `onEnable` hooks run. A command is
either a bean that extends `CommandAPICommand` or `CommandTree`, or a `CommandRegistrar` bean that
calls the existing DSL in `register()`. Suspending executors resolve their scope from the calling
plugin's context. A plugin with commands declares CommandAPI as a dependency with join-classpath:
Paper does not pass CommandAPI's classes on through surf-api, so without it the plugin cannot load
its command classes. The Gradle plugin adds that dependency.

## NATS

The NATS Gradle plugin generates a suspending interface for every service in a `.proto` contract.
A class annotated with `@NatsService` implements one or more of them; `@NatsService` is
meta-annotated with Spring's `@Component`, so the class needs no further stereotype:

```kotlin
@NatsService
class ShopRpcImpl(private val economy: EconomyApi) : ShopRpc {
    override suspend fun buy(request: BuyRequest): BuyResponse = TODO()
}
```

A bean post-processor in each plugin context registers the bean's final Spring proxy for every
generated service interface it implements; `@NatsService(ShopRpc::class)` narrows the set. A class
annotated with `@NatsService` that implements none fails the start. `@NatsListener` methods are
found on any bean. Registration happens in the enable phase. When a context closes, its handlers
drain and unregister before its dependencies close.

Further behavior is a separate annotation rather than an attribute of `@NatsService`, so the
annotation stays small as features are added. `@NatsTransactional` marks a microservice handler
whose database writes, deduplication claim and stored reply commit atomically through
`AtomicOperations`. The semantics come from the reliability module's
`NatsServiceInterceptorProvider`, and the start fails without that module.

A consumer injects a generated service interface directly to call the remote service:

```kotlin
@Service
class ShopFacade(private val shop: ShopRpc)
```

surf-api registers a service proxy for every generated service interface that a bean of the context
asks for. The proxy is created on first use, so the connection still opens only on use. Service
proxies are registered as Spring `@Fallback` beans: when the same context also implements the service, the local
implementation wins without ambiguity.

The NATS connection is shared and reference-counted. It opens when the first plugin that uses NATS
starts and closes when the last one stops. The root context never connects by itself. The platform
modules are bundled into the surf-api plugin; see [Redis](#redis) for the activation rule.

What the integration in `surf-nats-spring` (package `dev.slne.surf.api.nats`) does per plugin:

- It registers a lazy `@Fallback` `NatsClient` bean. The client's name is the plugin's name in lower
  case with every other run of characters replaced by `-`; it is visible on the wire as the queue
  group of the plugin's listeners. Two plugins whose names normalize to the same client name fail.
  A plugin that needs another name declares a `NatsClient` bean of its own.
- A plugin uses NATS if it implements a service, has a `@NatsListener` bean or injects a client or a
  service proxy. Its client connects right after the context refreshes, before `onLoad`. A client or service proxy
  created later, through an `ObjectProvider` for instance, connects while it is created, which
  blocks the caller briefly.
- Services and listeners are registered after the `onEnable` hooks and stop accepting work before
  the `onDisable` hooks, so requests in flight finish while the plugin's beans still exist. The
  client drains and closes when the plugin's context closes. Beans created lazily after the enable
  phase are not registered.
- Service proxy detection reads constructor parameters, `@Bean` method parameters and injected fields and
  setters, including `ObjectProvider`, `Optional` and `Provider` wrappers. A programmatic `getBean`
  and collection injection find no service proxy.
- `@NatsListener` methods are found on the bean's own class, also behind a CGLIB proxy; a JDK proxy
  fails the start.

NATS is not available in Paper's bootstrap phase. The configuration is `nats/config.yml` in
surf-api's own data folder. While that folder holds no configuration, surf-api copies the
`config.yml` from the data folder of a separately installed surf-nats platform plugin, such as
`surf-nats-paper`, into it.

## Redis

The Redis platform integration ships inside surf-api's Paper and Velocity plugin jars and its
Minestom runtime rather than as separate plugins. The `infra/redis` jars arrive with Redisson and
its Netty already relocated, and without Redisson's `META-INF/spring.handlers`, `spring.schemas`
and `spring.tooling`, which name a handler class that relocation leaves out of the jar.

Activation follows use:

- Every plugin context holds a lazy `RedisApi` bean created with
  `RedisApi.create(pluginName, serializersModule)`, where the module combines the plugin's
  `SerializersModule` beans. It is instantiated only when a bean of the plugin injects it.
- The physical Redisson connection is shared between all `RedisApi` instances of the process and
  opens on first use.
- surf-api calls `freeze()` and `connect()` for a plugin's `RedisApi` right after the plugin
  context refreshes, on `Dispatchers.IO`. Everything is connected before `onEnable`.

Beans create sync structures and simple caches while the context refreshes, in a constructor,
a property initializer or a `@Bean` method:

```kotlin
@Service
class OnlineService(redis: RedisApi) {
    private val online = redis.createSyncSet<UUID>("online-players")
}
```

Creating a structure after `connect()` throws an exception instead of blocking.

`RedissonClient` and `RedissonReactiveClient` are registered as surf proxies that forward to the
connected client. A call before the plugin's context has connected, in a constructor or an
initializer, or after the plugin was disabled, throws an exception that says Redis is not connected.

The connection is made right after the context refreshes, before `onLoad`. A `RedisApi` requested
after that point, through an `ObjectProvider` for instance, fails instead of never connecting.
Redis is not available in Paper's bootstrap phase.

The Spring integration lives in `surf-redis-spring`, package `dev.slne.surf.api.redis`, which
surf-api's root scan covers. Its configuration is `redis/config.yml` in surf-api's own data folder,
seeded the same way as the NATS configuration from a surf-redis platform plugin's data folder, such
as `surf-redis-paper`.

Microservices do not use Redis. `surf-redis-standalone` serves other standalone applications.

## Microservices

A microservice is a Spring Boot application with a `@SpringBootApplication` main class whose
`main` calls `runMicroservice<App>(args)`. Spring Boot owns the launch, the lifecycle and the order
of the integrations.

- The Boot context is surf-api's root context and hosts the application in the same context
  (`SurfApiStandaloneBootstrap.adopt`): surf-api's package is scanned, every `ContextContributor`
  found through `ServiceLoader` contributes (config handles, NATS), and Boot scans the application.
  A `SmartLifecycle` bean in phase 0 runs the load and enable hooks and the phase listeners when the
  context starts and the disable hooks when it stops; a failing start shuts surf-api down first.
- `MicroserviceInfo(name, dataPath)` is a bean. The name comes from `SURF_MICROSERVICE_NAME`, then
  `spring.application.name`, then the class name without `Application`/`Microservice`. The runtime
  publishes it as `spring.application.name` and the data folder (`SURF_MICROSERVICE_CONFIG_DIR`,
  default `config`) as `surf.config-directory`, which the database and NATS read.
- NATS uses the same contributor as plugin contexts; the client is named after the microservice, and
  a `NatsClient` bean of the application wins over that fallback, which keeps a pre-4.0 client name
  (see the NATS client name in `MIGRATION-4.0.md`). `@NatsTransactional` service beans get the
  reliability module's interceptor through `NatsServiceInterceptorProvider`; the stored replies
  belong to the microservice name.
- Health checks are Spring Boot `HealthIndicator` beans (`spring-boot-health`, no Actuator
  endpoint). Boot's own indicators are off by default (`management.health.defaults.enabled`). The
  `health` command and `microservicectl health` read the `HealthContributorRegistry`.
- The command channel (stdin, the Unix socket and `microservicectl`) starts after the hooks and
  stops before them. spark starts before them.
- The `exit` command closes the context from the main thread and exits with `0`; a failed start
  exits with `1`; a bean that closes the context with `SpringApplication.exit` sets the exit code
  from the `ExitCodeEvent`; a JVM signal closes the context in Spring Boot's shutdown hook. Boot's
  logging system is off (`LoggingSystem=none`) and log4j shuts down last through a Boot shutdown
  handler.
- The Gradle plugin sets the `Main-Class` of the shadow jar from the compiled classes: the single
  class with a `public static void main(String[])`, or `surfMicroservice { mainClass }`; none or
  several fail the build. The runtime is an `implementation` dependency, because `main` calls
  `runMicroservice`. The shadow jar merges `META-INF/services`, `META-INF/spring/*.imports` (line by
  line) and `META-INF/spring.factories` and `aot.factories` (per key, values joined with `,`). The
  surf-nats plugin adds `surf-api-nats-spring`.

## Database

The database library is built on Exposed with R2DBC and serves microservices only.

- `SurfDatabaseAutoConfiguration` (package `dev.slne.surf.database.spring`, listed in the jar's
  `AutoConfiguration.imports`) provides `DatabaseConfig`, `SurfMigrationRunner` and the
  `R2dbcDatabase`. `AtomicOperations` takes the `R2dbcDatabase`.
- `DatabaseConfig` is read once from `<surf.config-directory>/database.yml` (default `config`)
  with the `SURF_DATABASE_*` overrides, unless the application defines the bean. It is not a
  `ConfigHandle`: the pool cannot be reconfigured while it runs, and saving a handle would write
  the overrides, the password included, into the file.
- Every `MigrationSet` bean runs in `@Order` order before the `R2dbcDatabase` bean exists. The
  sets' checks and `ExposedMigration`s use the database before it is handed out. A failed
  migration fails the start and releases the pool. `ReliabilityMigrationSet` is the library's
  reliability schema; the module that uses `AtomicOperations` registers it.
- The runner supports named sets, per-set history tables, `{vendor}` locations,
  `baselineOnMigrate`, `failOnMissingLocations` and UTC sessions, on its own JDBC data source.
- The pool and the migration data source are not beans. With R2DBC, Flyway and Exposed relocated,
  Spring Boot's R2DBC and Flyway auto-configurations never match, so none is excluded.
- `R2dbcDatabase.retryingTransaction` runs the whole transaction again after a transient conflict:
  deadlock or serialization failure (PostgreSQL `40P01` and `40001`, MariaDB `1213`), and on
  MariaDB under REPEATABLE READ a row changed since the snapshot (`1020`, from
  `innodb_snapshot_isolation`). `Throwable.isTransientConflict()` exposes the check.
- Exposed's implicit retries are off: `SurfDatabaseAutoConfiguration` sets `defaultMaxAttempts = 1`,
  so a plain `suspendTransaction` runs its block once and a block with side effects outside the
  database never repeats unnoticed. Retries go through `retryingTransaction`. The setting is applied
  before the `SurfDatabaseConfigurer` beans, so a configurer can set `defaultMaxAttempts` back to
  Exposed's default of 3.
- The pool is r2dbc-pool. Defaults are `READ_COMMITTED`, UTC sessions and a lock timeout per
  pool (`pool.timeouts.lock-timeout-millis`, default 10 s, `SURF_DATABASE_POOL_LOCK_TIMEOUT_MILLIS`;
  MariaDB rounds it up to whole seconds). Flyway's connections keep the server default, so a
  migration waits for the locks of running transactions.
- `AtomicOperations` and `TransactionalInterceptor` are wired as beans and through the NATS
  post-processor.
- Integration tests run against MariaDB 12.3 and PostgreSQL 18.

## Build and distribution

- Spring, `kotlin-reflect` and `kotlinx-coroutines-reactor` ship inside the surf-api jar under
  their original package names. Plugins see them through `joinClasspath` and compile against them
  with `compileOnly`. A plugin that bundles Spring fails its build.
- The Gradle plugins apply `kotlin("plugin.spring")` and enable `javaParameters`.
- Spring versions come from the Spring Framework BOM, which `surf-api-spring-api` exports as an
  `api` platform; `surf-api-bom` aligns the surf-api modules.
- Components are found by Spring's classpath scan; there is no generated bean index. The surf KSP
  processor generates only the `@GenerateReflection` accessors and the NMS provider index under
  `META-INF/surf-api/nms-providers/`. The surf Gradle plugins run it on every project they
  configure, the `infra/` modules included. surf-api's own modules, which build on
  `core-convention` instead, run it only when they apply the `surf.ksp-processor` convention, as
  the Paper NMS version modules do.
- The Paper and Velocity plugin jars relocate jnats and the BouncyCastle it signs NKey challenges
  with to `dev.slne.surf.nats.libs` (`relocateNats()` in `buildSrc`) and leave out BouncyCastle's
  `libbc-*` native libraries, which JNI binds to the original class names; BouncyCastle then uses
  its Java implementation. The shadow jars of `infra/nats`, such as the `surf-api-nats-standalone`
  `-all` jar, apply the same relocation. Minestom has no surf-api plugin jar, so `io.nats` and
  BouncyCastle stay unrelocated on the server class path.
- The Paper plugin jar does not bundle Guava, Gson or `slf4j-api`; the server provides them.
- The api modules export neither Reactor Netty nor Netty. The Minestom and standalone runtimes ship
  only `netty-buffer` and `netty-common`, versioned by `netty-bom`, because packetevents links
  against `ByteBuf`; on Paper and Velocity the server's Netty is used.
- surf-api neither depends on DataFixerUpper nor relocates `com.mojang.serialization` or
  `com.mojang.datafixers`, so a plugin's DataFixerUpper references bind to the server's copy on
  Paper.
- Modules built on `core-convention` publish no `-all` (`shadowRuntimeElements`) variant to Maven,
  because a shaded copy would carry unrelocated Spring, Netty and Kotlin next to the consumer's
  own. `assemble` builds the Paper, Velocity and standalone runtime jars into `build/libs`.
  The explicit `shadow` publications of the `infra/` modules, such as `surf-api-redis-api`, publish
  their shaded jar.
- The `checkContextContributors` task of the Paper, Velocity and Minestom implementation modules,
  run by `check`, fails when the merged `META-INF/services` file of the shadow jar lacks the
  config, NATS or Redis `ContextContributor`; a missing one would only show as missing beans at
  startup.

## Rejected alternatives

| Alternative | Reason for rejecting it |
|---|---|
| A Spring Boot root context on Minestom | The root is the parent of every plugin context, so its auto-configured beans, such as the task executor, SSL bundles and the availability state, and its properties and profiles would reach every plugin. Boot also reconfigures logging and wants its own shutdown hook. After excluding what plugins must not see, only property binding, lifecycle and placeholder support remain, which surf-api does not use. |
| A Spring Boot root context in `SurfApiStandaloneBootstrap.bootstrap` | Boot's defaults (no circular references, no definition overriding, strict placeholders) would apply to the hosted application's own beans; `spring-boot` turns off log4j's shutdown hook JVM-wide; every standalone fat jar would need Spring registry merging; every test JVM would start a full Boot application. `adopt` already serves programs that want Boot. |
| Spring Boot on Paper, Velocity and Minestom for `@ImportAutoConfiguration` in plugins | No plugin needs a Boot auto-configuration; shipping `spring-boot` and `spring-boot-autoconfigure` would add about 1.8 MB to every platform jar. It can be added later without breaking anything. |
| Full Spring Boot in plugin contexts | Boot's logging switch (`LoggingSystem=none`) is JVM-wide, auto-configuration discovery can reach the jars of parent plugins, `@ConditionalOnMissingBean` searches ancestors, and the startup cost grows with the plugin count. |
| A provider's full context as the consumer's parent | Works for one dependency only and exposes the provider's internals, such as its `DataSource`. |
| A bean factory that delegates to several provider factories | Correct candidate resolution for generics, qualifiers and aliases amounts to a second DI implementation. |
| `@Service` on the exported interface | Spring's component scan only picks up concrete classes, so the annotation has no effect. |
| Spring Data JDBC repositories next to Exposed | A second mapping for every table, a shared transaction with Exposed that would first need proof, pessimistic locking without `SKIP LOCKED` or `NOWAIT`, conditional atomic updates and upserts only as SQL strings, and Exposed writes that bypass `@Version`. |
| jOOQ | The open-source edition supports only the latest database versions (MariaDB 11.5 and PostgreSQL 18 at the time of the decision), code generation needs a database during the build, and the team would learn a new API. |
| JDBC with virtual threads instead of R2DBC | Without Spring Data and jOOQ, the only remaining gain is Spring's `@Transactional`, which does not apply to suspending functions. Suspending code inside a transaction would need a thread-confinement workaround, and `AtomicOperations` would need a redesign. Exposed's Spring integration exists only for JDBC, but surf code does not need Spring transactions. |
| A `@SuspendTransactional` annotation | Depends on confining the coroutine to one thread; a `withContext` switch inside the method would silently leave the transaction. Obsolete with R2DBC. |
| Spring Cache backed by Redisson | No use case; `SimpleRedisCache` covers the existing needs. |
| The Redisson Spring Boot starter | Requires Spring Data Redis and references `org.redisson`, while surf relocates Redisson. |
| Spring Boot's Flyway auto-configuration | Supports one Flyway instance with one history table, defaults to `baselineOnMigrate=false`, does not enforce UTC sessions and manages an older Flyway (12.x) than the one in use (13.x). |
| Replacing Configurate with Spring configuration | Spring only binds values; see [Configuration](#configuration). |
| A KSP-generated bean index (the 3.x mechanism) instead of Spring's classpath scan | Spring scans the plugin jar itself; a generated index would be a second source of truth that the processor has to regenerate on every change and that goes stale when it does not. |
| Generating the main class at runtime | Paper loads the main class by name from the plugin jar, so the class must exist at build time. |
| Relocating Spring | Spring refers to its own classes by name in strings, `spring.factories` and `.imports` files, and plugins would have to compile against relocated annotations. |
| mccoroutine for plugin scopes and dispatchers | `SuspendingJavaPlugin` conflicts with surf's base classes owning the lifecycle, and the entity context and plugin scopes are surf code anyway. |
| Shipping DataFixerUpper with a corrected relocation | surf-api itself does not use it, so dropping the dependency and its API is cheaper than keeping its relocation correct for Paper, whose server ships its own copy. A plugin that needs DataFixerUpper declares it itself. |

## Verification on a real server

`local/paper-shared-bean-test` boots Paper 26.3 with surf-api and two test plugins and asserts the
following; the plain JVM tests of `surf-api-spring-impl` cover the same contracts in one class
loader:

- a consumer injects a provider's `@PluginApi` bean by constructor, and both see the same
  interface class; the provider's private beans stay invisible
- component scanning from a package both plugins share registers only the consumer's own classes
- the thread context class loader is the plugin's while its beans are created
- no context registers a JVM shutdown hook, and Spring logs through the server's SLF4J
- `@ConditionalOnPlugin` skips an adapter for a missing plugin without loading its class
- `@Configuration` classes get their CGLIB subclass inside Paper's class loader
- the phases run bootstrap, load and enable provider first, disable consumer first, and the export
  is destroyed once, by its provider
- a coroutine launched in `onLoad` outlives the load phase, and a suspending command starts in the
  declaring plugin's scope on the server thread
- a `@SurfConfig` class in a subfolder of the data folder is read through its `ConfigHandle`
- a disable hook can cancel and join a coroutine that waits for server thread work
- against Redis and NATS in Docker containers, a plugin that injects `RedisApi` and `RedissonClient`
  reads back a sync value and runs a Redisson command, and a plugin with a `@NatsService` calls its
  own service through a service proxy and receives a published event over the broker

The boot test also covers a component extending a missing plugin's class, a `@Bean` method of a
present plugin's API type, a bootstrap bean with a `@BootstrapComponent` dependency, and the
plugin proxy comparing equal to the instance. It fails on any context failure in the log.

`local/velocity-shared-bean-test` does the same on a Velocity proxy without Redis and NATS: the
constructor-injected export, private beans, the plugin instance, the thread context class loader,
`@ConditionalOnPlugin`, CGLIB configuration classes, the Velocity and surf event listeners, the
absence of JVM shutdown hooks, a command bean run from the console, and the phase order.

Both boot tests download the server or proxy and three third-party plugins from pinned URLs with
checksums. A missing network, a moved release or a changed checksum aborts the test, which Gradle
reports as skipped; the Paper test also skips without Docker. Neither test belongs to `check`,
`test` or CI; run them by name.

Three properties of Paper and Kotlin shape the design:

- Paper's plugin class loader loads classes through the plugins it joins but returns `.class`
  resources only from the plugin's own jar. Spring reads the bytecode of a bean's interfaces as
  resources, so a plugin context resolves a missing `.class` resource through the class loader that
  defined the class.
- In the bootstrap phase a plugin cannot see Bukkit plugins, which is why a Paper plugin has a
  separate bootstrap context.
- Kotlin compiles a forwarding method for every interface default into each implementing class,
  which is why `onBootstrap` lives in an interface of its own.

### Verification gate

A change to the Spring integration, the packaging or the Gradle plugins is verified by the
following steps.

Before you begin:

- Start Docker. The Testcontainers integration tests, annotated with
  `@Testcontainers(disabledWithoutDocker = true)`, and the Paper boot test skip without it.
- Create `vendor/LuckPerms` (`git submodule update --init --recursive`, then
  `cd vendor && ./gradlew applyPatches`). Without it, `surf-api-minestom-luckperms-impl` is not
  built.
- Allow network access: the boot tests download their server and plugins, and the Minestom example
  server downloads its libraries into `run/libraries/` when it starts.
- Behind a TLS-intercepting proxy on Windows, set
  `GRADLE_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` and append
  `"-Psurf.bootTest.jvmArgs=-Djavax.net.ssl.trustStoreType=Windows-ROOT"` to the boot test
  commands in steps 4 and 5, which pass it to the test JVM and to the server process.
- Add `--no-daemon --max-workers=3` to every `./gradlew` command. `gradle.properties` turns on the
  daemon and parallel execution: on Windows a daemon that outlives the build keeps paperweight's
  mapped server jar locked, and more workers run enough compile and test processes next to the
  8 GB Gradle heap to exhaust the machine's memory.

Run the steps one after another, never two Gradle invocations in parallel:

1. Run the `buildSrc` tests:

   ```shell
   ./gradlew -p buildSrc test
   ```

2. Check the ABI dumps:

   ```shell
   ./gradlew checkKotlinAbi
   ```

3. Build everything with tests:

   ```shell
   ./gradlew --continue build
   ```

   Then read every `**/build/test-results/**/TEST-*.xml` and confirm that `failures`, `errors` and
   `skipped` are all `0`. A skipped test does not count as passed, because the Docker-gated tests
   skip silently.

4. Run the Paper boot test and confirm one passed test and no skipped one:

   ```shell
   ./gradlew :surf-api-paper-shared-bean-test:paperSharedBeanBootTest
   ```

5. Run the Velocity boot test and confirm the same:

   ```shell
   ./gradlew :surf-api-velocity-shared-bean-test:velocitySharedBeanBootTest
   ```

6. Build the Minestom example server and check its service file:

   ```shell
   ./gradlew :surf-api-minestom-example:shadowJar :surf-api-minestom-impl:checkContextContributors
   ```

   Copy `local/minestom-example/build/libs/surf-api-minestom-example-VERSION-all.jar` into an empty
   directory and start it there with `java -jar`. Replace `VERSION` with the version of the build.
   Confirm that the log shows `Example server started on port 25565`, type `stop`, and confirm
   `Example server stopped` with no `ERROR` line and no exception in the log.

7. Check the dumps and the plugin jars:
   - Every deletion in `git diff HEAD -- '*.api'` is intended and has an entry in
     `MIGRATION-4.0.md`.
   - The `-all` jars of `surf-api-paper-impl` and `surf-api-velocity-impl` contain no entry under
     `io/netty/`, `reactor/netty/`, `net/megavex/`, `io/nats/` or `org/bouncycastle/`; the
     relocated copies live below `dev/slne/`.
   - The `-all` jar of `surf-api-nats-standalone` contains no entry under `io/nats/` or
     `org/bouncycastle/`, and its `META-INF/services/java.security.Provider` names only classes
     below `dev.slne.surf.nats.libs.`.
