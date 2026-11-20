# Lifecycle, scopes and shutdown

Sources: `foundation/spring/api/.../spring/SurfLifecycle.kt` (`SurfLifecycle`,
`SurfBootstrapLifecycle`), `BootstrapComponent.kt`, and in `foundation/spring/impl/.../spring/context/`
`LifecycleHookRunner.kt`, `PluginContext.kt`, `ContextPhaseListener.kt`, `ContextCoroutines.kt`,
`SurfContextManager.kt`. The platform runtimes are `PaperSpringRuntime`, `VelocitySpringRuntime`,
Minestom's `PluginManagerImpl` and `SurfApiStandaloneBootstrap`.

## The hooks

```kotlin
interface SurfLifecycle {
    suspend fun onLoad() {}
    suspend fun onEnable() {}
    suspend fun onDisable() {}
}

interface SurfBootstrapLifecycle {
    suspend fun onBootstrap()
}
```

A bean implements the interface and overrides what it needs. `SurfBootstrapLifecycle` is an
interface of its own because implementing it decides which context the bean lives in on Paper.

## When each phase runs

| Platform | Context refresh | `onLoad` | `onEnable` | `onDisable`, then close |
| --- | --- | --- | --- | --- |
| Paper | plugin bootstrapper, followed by `onBootstrap` | plugin `onLoad` | plugin `onEnable` | plugin `onDisable` |
| Velocity | plugin constructor | plugin constructor | `ProxyInitializeEvent` | `ProxyShutdownEvent` |
| Minestom | plugin manager load | plugin manager load | plugin manager enable | teardown |
| Standalone | `bootstrap` (root only) | `bootstrap` | `enable` | `shutdown` |

On Paper the full plugin context refreshes in the load phase, below its import context, which also
references the plugin's bootstrap beans; see [Paper's bootstrap context](#papers-bootstrap-context).

Before the `onEnable` hooks run, surf-api registers the plugin's listener and command beans:
Bukkit `Listener` beans on Paper, `@Subscribe` beans on Velocity, `EventNode` beans on Minestom,
`@SurfEventHandler` beans everywhere, and `CommandAPICommand`, `CommandTree` and `CommandRegistrar`
beans. A hook can therefore rely on them. On Velocity, listener beans are registered while
`ProxyInitializeEvent` fires, so their own handlers for that event never run; use `onEnable`.

## Order within a context

- `onBootstrap`, `onLoad` and `onEnable` run in the order in which the beans finished
  initializing. A bean finishes after its dependencies, so dependencies run first.
- `@Order` or `Ordered` moves a bean ahead of the beans that declare none. The hooks read the order
  from the bean's class only: `@Order` on a `@Bean` method sorts collection injection, not the
  hooks. To order a `@Bean`'s hooks, annotate its class or implement `Ordered`.
- `onDisable` runs in exactly the reverse order.
- The hooks of one phase run one after another, never concurrently.
- A throwing `onBootstrap`, `onLoad` or `onEnable` aborts the phase and fails the plugin; a Paper
  plugin whose load failed is never enabled. A throwing `onDisable` is logged and the remaining
  hooks still run.

Only singletons that exist when a phase starts take part. A `@Lazy` bean created later misses the
phases that already ran, and a prototype bean never takes part.

The hooks block the thread that calls the phase; on Paper that is the main thread for load, enable
and disable. Paper runs each of these phases inside `PluginDispatchers.lifecycle`, which routes the
`PaperDispatchers.main` work of every plugin into the blocked phase, so a hook may wait for work on
`main`. On a server without regionized ticking the same holds for `region(...)` and `entity(...)`;
on Folia that work goes to Paper, and a hook must not wait for it. A hook must not wait for a task
handed to the Bukkit scheduler directly; launch such work without waiting.

Hooks run with the elements of the plugin's coroutine scope except its job, dispatcher and
exception handler, so the plugin's class loader is the thread context class loader on every thread
they resume on.

## Phase listeners

`ContextPhaseListener` (`surf-api-spring-impl`) has `afterEnable()` and `beforeDisable()`. Beans of
a plugin's full context that implement it run after every `onEnable` hook and before the first
`onDisable` hook. surf-api modules contribute them through a `ContextContributor`; NATS uses one to
serve a plugin's endpoints only while its beans are alive. A failing `afterEnable` fails the enable
phase; a failing `beforeDisable` is logged.

## Paper's bootstrap context

Paper builds a plugin's first context in the bootstrap phase, when the plugin sees only the classes
of its bootstrap dependencies and Paper has not created the plugin instance. Refreshing the whole
context then would fail, because Spring inspects every bean class during a refresh. A Paper plugin
therefore has two contexts:

| Context | Built in | Holds |
| --- | --- | --- |
| bootstrap context | bootstrap phase, below surf-api's root bootstrap context | the components that implement `SurfBootstrapLifecycle` or carry `@BootstrapComponent`, found from bytecode without loading other classes |
| full plugin context | load phase, below the import context | every other component; injects the bootstrap beans |

```kotlin
import dev.slne.surf.api.spring.BootstrapComponent
import dev.slne.surf.api.spring.SurfBootstrapLifecycle
import org.springframework.stereotype.Service

@Service
class ChannelRegistrar(private val names: ChannelNames) : SurfBootstrapLifecycle {
    val registered = mutableListOf<String>()

    override suspend fun onBootstrap() {
        registered += names.all()
    }
}

@BootstrapComponent
class ChannelNames {
    fun all() = listOf("shop", "trade")
}
```

A bootstrap bean depends only on surf-api's bootstrap beans and other bootstrap components. It
must not touch a non-bootstrap plugin's API (LuckPerms, PlaceholderAPI), the Bukkit server API or
the concrete plugin class, and must not dispatch to the main or a region thread, which do not tick
yet. It may hold the plugin as `org.bukkit.plugin.Plugin`: a proxy that forwards to the instance
from the load phase on and compares equal to it. Paper's `BootstrapContext` is injectable there.
The bootstrap beans' hooks run before those of the full context, and `onDisable` in reverse.

On Velocity, Minestom and the standalone runtime `@BootstrapComponent` is an ordinary component of
the single context, and `onBootstrap` runs when that context loads, right before its `onLoad` hooks.
The Paper restrictions above do not apply there.

## The plugin instance

The plugin's main object is the primary bean `surfPluginInstance` of its full context, under its
concrete type and every supertype. Spring manages it like any singleton: it injects `@Autowired`
fields and setters, runs the `Aware` callbacks, `@PostConstruct` and every `BeanPostProcessor`, and
calls `@PreDestroy` and `DisposableBean.destroy()` when the context closes. Only an inferred
`close()` or `shutdown()` call is suppressed. Cleanup written into both a disable path and
`@PreDestroy` runs twice.

## Coroutine scopes

Every full plugin context and every root holds a `CoroutineScope` bean named `surfCoroutineScope`,
built by `surfCoroutineScope(owner, classLoader, dispatcher, elements)`:

- a `SupervisorJob`, `Dispatchers.Default`, `CoroutineName("surf-<owner>")` and the plugin's class
  loader as thread context class loader
- a handler that logs an uncaught failure through the commons-logging logger `surf-coroutines` as
  `Unhandled exception in a coroutine of '<owner>'`

The owner is the plugin's name on Paper and its id on Velocity and Minestom. On Minestom the plugin
manager creates the scope before the plugin's constructor runs, and `SurfMinestomPlugin.scope`
returns it. A Minestom plugin without a jar of its own, such as one shaded into the server, runs its
scope's coroutines and its lifecycle hooks with the server's class loader as thread context class
loader. On Paper the scope also carries the plugin's `PaperDispatchers`.

The plugin's scope is primary, so a plugin bean that injects `CoroutineScope` gets its own scope,
not the root's. The plugin context owns it: it cancels the scope after the disable hooks ran, when
the contexts close without them, or when the context cannot be built. Paper's bootstrap contexts
have no scope.

## Shutdown

`onDisable` hooks run, the scope is cancelled, then the plugin's contexts close: the full context,
its import context, then its bootstrap context. Closing destroys the plugin's own beans, exports
included, through Spring's `@PreDestroy`, `DisposableBean` and inferred destroy methods. The import
context only drops its references.

A provider whose exports are still imported by an open plugin keeps its contexts open until the
last importer closes, so no consumer ever holds a destroyed bean. At server shutdown the contexts
close last-started first, so consumers close before their providers, and the root closes last. No
context registers a JVM shutdown hook, and there is no hot reload. The standalone runtime's
`enable()` registers a JVM shutdown hook of its own that calls `shutdown()`, unless it is called
with `registerShutdownHook = false`.

## `BeanPostProcessor`

Plugins use Spring's `org.springframework.beans.factory.config.BeanPostProcessor` unchanged. A
post-processor is scoped to the context that declares it: one in a plugin context sees only that
plugin's beans, and the root's post-processors never see plugin beans. surf-api installs its
per-plugin processing, such as the lifecycle hook runner and the NATS registration, in each plugin
context for that reason. Spring creates post-processors before the other beans, and a bean a
post-processor injects is created too early to be processed itself; take such a dependency as
`ObjectProvider<T>`.
