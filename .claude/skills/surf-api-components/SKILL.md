---
name: surf-api-components
description: Use when writing or wiring a surf-api bean - annotating a class with @Service, @Repository or an own @BeanMeta stereotype, choosing a Scope, publishing a third-party or private-constructor type with @Bean, shaping a constructor parameter as T, T?, List<T>, Set<T> or Lazy<T>, implementing SurfComponent load/enable/disable or a BeanPostProcessor, guarding a bean with @ConditionalOn* or @DependsOnBean, disambiguating with @Qualifier, @Primary or @Priority, reaching for BeanContainer at a platform entry point, or diagnosing "Cyclic dependency between beans", "is provided by several beans" or "but no bean provides it".
---

# surf-api beans and the bean container

Every claim below comes from `foundation/di/api/src/main/kotlin/dev/slne/surf/api/di/`. Read those
files when in doubt; they are the only source of truth.

## A bean is a class carrying `@BeanMeta`

Directly, or through an annotation that itself carries `@BeanMeta`. `@Service` and `@Repository` are
exactly that - meta-annotated aliases that read better at the declaration. A bean needs **no
supertype and no interface**; annotating the class is all there is to it.

```kotlin
@Service
class ShopService(
    private val transactions: TransactionApi,
    private val handlers: List<PriceHandler>,
)
```

Both aliases take one parameter, `scope`, defaulting to `Scope.SINGLETON`:
`@Service(scope = Scope.SHARED)`. `@Repository` behaves identically to `@Service`; the separate name
only says the bean's job is storage.

A project defines its own stereotype the same way:

```kotlin
@BeanMeta
@Priority(100)
@Target(AnnotationTarget.CLASS)
annotation class Projection

@Projection
class BalanceProjection(private val cache: BalanceCache)
```

Lifecycle is **separate and opt-in** through `SurfComponent` - a bean with nothing to open or close
does not implement it.

The container is eager: starting it constructs every non-`PROTOTYPE` bean it owns. Which classes
those are comes from compile-time metadata, one file per module under `META-INF/surf-api/beans`,
merged across the whole classpath (`definition/BeanMetadata.kt`).

## Constructor injection: four parameter shapes

The shape of the parameter decides how it is resolved.

| Parameter | Resolved as |
| --- | --- |
| `T` | exactly one bean. No candidate fails; several candidates with nothing to pick between them fail. |
| `T?` | the bean, or `null` when no container in the hierarchy provides it. This is how an optional integration avoids needing a condition. |
| `List<T>` / `Set<T>` | **every** candidate: this container's, ordered by `@Priority` then class name, followed by the parents'. Empty is a valid answer, never a failure. |
| `Lazy<T>` | resolved on first `.value`, not at construction. The deliberate - and only - way to break a dependency cycle. |

A bean is resolvable under its own class and every supertype except `Any`, so declare the parameter
as the interface and let any implementation satisfy it.

`T` and the collection shapes create edges in the dependency graph, so what a bean needs exists
before it is constructed. `Lazy<T>` and an absent optional create no edge.

Write what the bean needs into its constructor. Reach for `BeanContainer` itself only at the edges,
where a platform hands control back and there is no constructor to inject into.

Detail, including the hierarchy walk and the `BeanContainer` lookup functions:
`references/injection.md`.

## `@Bean` publishes what the container cannot construct

A third-party type, a type with a private constructor, anything only a factory can produce: annotate
a property or a function of a bean, and its value becomes injectable on its own.

```kotlin
@Service(scope = Scope.SHARED)
class RedisConnection : SurfComponent {

    @Bean(scope = Scope.SHARED)
    val api: RedisApi = RedisApi.create()

    override suspend fun enable() = api.freezeAndConnect()
    override suspend fun disable() = api.disconnect()
}
```

Consumers inject `RedisApi` and never learn that `RedisConnection` exists.

- The declaring class must itself be a bean. A factory **function** may take parameters, resolved
  like any other injection point; the factory is always created after its declaring bean.
- **The trap: the published type is the _declared_ type.** Write the contract type on the property
  or as the function's return type. An inferred implementation type is what gets published, and
  consumers asking for the contract then fail.
- `@Bean(scope = ...)` defaults to `Scope.SINGLETON` - state `Scope.SHARED` explicitly when the
  value belongs to the whole process, even on a `SHARED` declaring bean.

## `Scope`

| Scope | Instances | Lifecycle |
| --- | --- | --- |
| `SINGLETON` (default) | one per container; another application's container holds its own | yes |
| `SHARED` | one per process, cached in the root container, so every container below resolves the same object | yes |
| `PROTOTYPE` | a new one for every injection point; nothing is cached | **no** |

A `PROTOTYPE` bean takes no part in the lifecycle: the container does not keep the instance long
enough to shut it down, so its `SurfComponent` methods are never called. Do not give one a resource
that has to be closed.

`SHARED` is for what genuinely exists once per process - a broker connection, a platform service -
and never for state belonging to one application, because every other application sees it too.

## The container hierarchy

A root container belongs to surf-api itself and holds what exists once per process. Below it sits
one container per application - a plugin, a microservice - holding that application's own beans.

Resolution **walks up only**: a container answers from its own beans, then asks its parent. It never
walks down and never sideways, so an application cannot reach into another's beans. A bean crosses
that boundary by being published into the root with `Scope.SHARED`, and only that way.

**A `SHARED` bean is published when its own application's container starts**, so another
application can inject it only if it starts afterwards. On Paper that means the consumer declares
the publisher under `dependencies.server` with `load: BEFORE` and `join-classpath: true` - which it
has to do regardless, because otherwise the interface is loaded by two classloaders and the instance
it receives cannot be cast to the type it asked for, with nothing to say so. Start the consumer
first and you get the ordinary "no bean provides" failure, which at least says what is missing.

One exception to "own beans first": a collection parameter is a union - it takes this container's
candidates *and* the parents', because a registrar wants every handler in the process.

## An application starts its own container

surf-api starts the root. Everything else asks for a child, from its own lifecycle, through
`SurfContainer` in `surf-api-di-api` - no implementation module needed:

```kotlin
class MyPlugin : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() { SurfContainer.startChild(this) }
    override suspend fun onEnableAsync() { SurfContainer.enableFor(this) }
    override suspend fun onDisableAsync() { SurfContainer.stopFor(this) }
}
```

`startChild` reads *this* application's bean metadata off its own classpath entry, builds the graph
and runs `load`. `enableFor` runs `enable`. They are two calls because the platform has two phases:
a bean's `enable` belongs in the server's enable phase, where the rest of the server exists, not in
its load phase. `stopFor` runs `disable` in reverse and forgets the container, so the same owner
could start again.

Pass the application itself as the owner - the plugin instance, not its class. The owner is what
the platform is asked about when the container needs a classloader, a logger or a data directory.

A failure here is a startup failure: a bean the container cannot build, a cycle, an ambiguity. It
throws rather than carrying on with a half-built container.

## `SurfComponent` is opt-in lifecycle

Implement it only when the bean owns something that must be opened and closed. The container runs
the phases as separate passes over all beans, each in dependency order:

1. every bean is constructed and injected
2. every bean passes through the `BeanPostProcessor` chain
3. `load()` on every bean

...then, in the platform's *enable* phase:

4. `enable()` on every bean

...and on shutdown:

5. `disable()` on every bean, **in reverse**

Steps 1-3 are `startChild`, step 4 is `enableFor`, step 5 is `stopFor`. The phases are not chained:
nothing runs `load` for you because you asked for `enable`.

`load`, `enable` and `disable` all suspend and all default to doing nothing, so override
only what you need.

**Why the gap between `load` and `enable` exists:** it is what makes a two-phase resource work. A
connection is created or opened in `load` while the beans that register against it are still being
set up, and is frozen or connected in `enable`, once every `load` has run. Put "open it" in `load`
and "no more registrations, go live" in `enable`. Reverse `disable` means a bean is torn down before
what it depends on, so a client closes before its connection.

Detail, plus shutdown behaviour on failure: `references/lifecycle.md`.

## `BeanPostProcessor` acts on beans nobody collected

A processor sees every bean right after construction and injection, and acts on what it finds -
methods, supertypes, annotations. That is how handlers get registered when what identifies a handler
sits on a *method*, not on the class, and so cannot be expressed as a type to inject.

Two rules that are not optional:

- **The dependency runs one way only.** The handler beans know the connection; the connection knows
  nothing about them; the processor sits beside both. Collecting the handlers into the connection's
  constructor instead inverts that, and the first handler that needs the connection is a cycle.
- **Take the registration target as `Lazy<T>`.** A processor that demands its target eagerly forces
  that bean to be created before the processor can run, which the container cannot promise for every
  ordering.

Worked example and the replace-a-bean contract: `references/lifecycle.md`.

## Conditions decide whether a bean is created at all

A bean whose condition does not hold is **skipped: never constructed, never injected**. An injection
point that requires it then fails. That is deliberate - a missing bean is a configuration error, not
something to paper over. Make the parameter `T?` if the absence is legitimate.

Every annotation is repeatable and every one has to hold. Put them on the class, or on an
annotation class so a stereotype carries the condition. They also target a property or function,
but guard the **declaring class** rather than a single `@Bean` member - see
`references/conditions.md`.

| Annotation | Holds when |
| --- | --- |
| `@ConditionalOnProperty(key = ["shop", "enabled"], havingValue = "true", matchIfMissing = true)` | the configuration property matches |
| `@ConditionalOnEnvironment(["production"])` | the process runs in one of these environments |
| `@ConditionalOnClass(SomeLibrary::class)` | the class is on the runtime classpath |
| `@ConditionalOnClassName("com.example.Other")` | same, for a class not on the compile classpath either |
| `@ConditionalOnPlugin("LuckPerms")` | the platform has that plugin loaded |
| `@ConditionalOnAnyPlugin(["Vault", "Treasury"])` | at least one of them is loaded |
| `@ConditionalOnMissingBean(Storage::class)` | nothing else provides that type - how a default is written |
| `@ConditionalOn(MyCondition::class)` | your own `BeanCondition` returns `true` |
| `@DependsOnBean(Migrations::class)` | not a condition: orders this bean after that one **without injecting it** |

Parameter names, defaults and `BeanCondition`: `references/conditions.md`.

## Ambiguity fails, it is never guessed

Needed only once two beans provide the same type.

- `@Qualifier("name")` on the declaration names the bean; `@Qualifier("name")` on the parameter names
  which one it wants. The strings must match. A qualifier on a bean does not hide it from an
  unqualified injection point.
- `@Primary` picks the bean when the injection point names none. At most one per provided type - a
  second one fails too.
- `@Priority(value)` only separates beans the graph leaves unordered, lower first. It can never move
  a bean before something it depends on. On a `BeanPostProcessor` it orders the chain instead.

```kotlin
@Service @Qualifier("primary")  class PrimaryStorage : Storage
@Service @Qualifier("fallback") class FallbackStorage : Storage

@Service
class Writer(@Qualifier("fallback") private val storage: Storage)
```

## The failures you will actually meet

| Message | Means | Do |
| --- | --- | --- |
| `Cyclic dependency between beans: A -> B -> A. Break it by taking one of these as Lazy<T>.` | the path is spelled out provider by provider | pick the step in that path where the value is genuinely needed later and make that parameter `Lazy<T>`. If both sides need each other at construction, the split is wrong - a `BeanPostProcessor` often removes the edge entirely |
| `<Type> is provided by several beans and none is marked @Primary: <ids>` | several candidates, nothing to choose between them | `@Primary` on the one that should win, or `@Qualifier` at the injection point. The ids name the candidates; a factory's id is `DeclaringClass#memberName` |
| `<Type> is provided by several beans marked @Primary: <ids>` | two `@Primary` for one type | remove one |
| `<Consumer> needs <Type>, but no bean provides it.` | the consumer is named first, the missing type second | annotate the implementation, publish it with `@Bean`, make the parameter nullable - or check whether a condition skipped the bean that would have provided it |
| `No bean provides <Type> in the container for <owner>.` | the same, from an explicit `bean()` lookup | as above; also check you are asking the right container - lookups never walk down or sideways |
| `The same bean is defined twice on the classpath: <names>` | one module arrived through two jars | fix the dependency graph, not the bean |
| `<Class> has no primary constructor, so the container cannot create it.` | nothing to call | give it a primary constructor, or publish the value with `@Bean` from a bean that can be created |

## References

Read on demand, not up front:

- `references/injection.md` - every parameter shape in full, the hierarchy walk, `@Bean` factories,
  qualifier/primary/priority rules, and the `BeanContainer` functions for the edges.
- `references/lifecycle.md` - the phases, what runs where, `BeanPostProcessor` worked example and
  contract, shutdown, and what `PROTOTYPE` opts out of.
- `references/conditions.md` - every `@ConditionalOn*` with its exact parameter names and defaults,
  `@DependsOnBean`, and writing a `BeanCondition`.
