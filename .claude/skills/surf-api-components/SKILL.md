---
name: surf-api-components
description: Use when writing or wiring a surf-api Spring bean - annotating a class with @Service, @Component or @Configuration, publishing a type with @Bean, shaping a constructor parameter as T, T?, List<T> or ObjectProvider<T>, implementing SurfLifecycle or SurfBootstrapLifecycle, exporting a bean to other plugins with @PluginApi, guarding an adapter with @ConditionalOnPlugin, choosing between candidates with @Qualifier, @Primary, @Fallback or @Order, putting a bean into Paper's bootstrap context with @BootstrapComponent, injecting what every plugin context provides (CoroutineScope, PaperDispatchers, the plugin instance, ConfigHandle), or diagnosing NoSuchBeanDefinitionException, NoUniqueBeanDefinitionException, BeanCurrentlyInCreationException or PluginContextException.
---

# surf-api beans on Spring

surf-api wires beans with Spring Framework. A plugin writes plain Spring components; surf-api
builds one `AnnotationConfigApplicationContext` per plugin and drives it through the platform's
phases. Sources:

- `foundation/spring/api/src/main/kotlin/dev/slne/surf/api/spring/` - what plugins write against:
  `SurfLifecycle`, `SurfBootstrapLifecycle`, `BootstrapComponent`, `PluginApi`, `SurfApiBean`,
  `ConditionalOnPlugin` (`PluginPresence` behind it is `@InternalSurfApi`, provided by the platform)
- `foundation/spring/impl/src/main/kotlin/dev/slne/surf/api/spring/context/` - how the contexts
  are built, exported and closed
- `docs/spring-migration.md` - the design decisions behind both

## The context tree

Every JVM holds these contexts:

| Context | Parent | Holds |
| --- | --- | --- |
| root | none | surf-api's own beans, such as `SurfEventBus`, `SpongeConfigSerializers` and `ViewFrame`. Scans `dev.slne.surf.api`. |
| import context, one per plugin | root | only the beans the plugin's **direct** dependencies export |
| plugin context, one per plugin | its import context | every bean of the plugin |

Paper adds a bootstrap context per plugin and a root bootstrap context; see
`references/lifecycle.md`.

Lookups walk up: plugin context, import context, root. A plugin therefore sees surf-api's beans
and its direct dependencies' exports, and never another plugin's private beans or the exports of a
dependency's dependency. Spring does not hand bean post-processors down to child contexts, so a
`BeanPostProcessor` acts only on the context that declares it.

## A bean is a Spring component

Annotate the class with a Spring stereotype: `@Service`, `@Component` or `@Repository` from
`org.springframework.stereotype`, or `@Configuration` for a class of `@Bean` methods. The plugin's
types below are its own:

```kotlin
import org.springframework.stereotype.Service

@Service
class ShopService(
    private val prices: PriceRepository,
    private val handlers: List<PriceHandler>,
)
```

Component scanning starts at the plugin's base package and reads only the plugin's own jar. The
surf Gradle plugin derives that package from the main sources (their longest common package),
generates the plugin's main class into it and accepts `basePackage` as an override. It rejects
`dev.slne.surf.api` itself and every package containing it, such as `dev.slne.surf`; on Minestom,
where a plugin can share surf-api's class loader, it also rejects packages inside
`dev.slne.surf.api`. The builds apply `kotlin("plugin.spring")`, which opens
stereotype and `@Configuration` classes for proxies, and compile with `javaParameters`.

The plugin's main class (`SurfPaperPlugin`, `SurfVelocityPlugin`, `SurfMinestomPlugin`) carries
no logic: surf-api drives the context through it. Put the logic into beans.

surf-api's own services are beans of the root context. Inject them by constructor like any other
bean. Their companion accessors, such as `SurfEventBus.instance` and `SurfApiCore.instance`, are
`@InternalSurfApi` and serve surf-api's static entry points.

## Constructor injection

The parameter's shape decides how Spring resolves it:

| Parameter | Resolved as |
| --- | --- |
| `T` | exactly one candidate. None fails with `NoSuchBeanDefinitionException`; several with nothing to choose between them fail with `NoUniqueBeanDefinitionException`. |
| `T?` | the candidate, or `null` when nothing provides `T`. Several candidates still fail. |
| `List<T>` / `Set<T>` | every candidate of this context and its ancestors, sorted by `@Order` or `Ordered`. With no candidate, the class's sole constructor gets an empty collection. |
| `ObjectProvider<T>` | resolved when called: `getObject()`, `getIfAvailable()`, `getIfUnique()`, `orderedStream()`. Postpones creation and breaks a constructor cycle. |

Kotlin's `Lazy<T>` is no injection shape; take `ObjectProvider<T>` instead, or annotate an
interface-typed parameter with Spring's `@Lazy`, which injects a proxy that resolves on first call.

Spring matches generic type arguments, so `ConfigHandle<ShopConfig>` and `Repo<Foo>` resolve
exactly. Detail, `@Bean` methods and programmatic lookups: `references/injection.md`.

## `@Bean` publishes what scanning cannot create

A third-party type, a type with a private constructor, or anything only a factory produces goes
into a `@Configuration` class:

```kotlin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ShopConfiguration {

    @Bean
    fun priceCache(prices: PriceRepository): PriceCache = CaffeinePriceCache(prices)
}
```

Declare the contract as the return type. Spring predicts a bean's type from it before the bean
exists, and an export carries that type to consumers. Method parameters are injected like
constructor parameters.

## Lifecycle hooks

`SurfLifecycle` gives a bean the suspending hooks `onLoad`, `onEnable` and `onDisable`; each
defaults to doing nothing. Within one context they run in the order in which the beans finished
initializing, so a bean's dependencies run first; `@Order` moves a bean ahead of the unordered
ones; `onDisable` runs in reverse. A throwing `onLoad` or `onEnable` fails the plugin; a throwing
`onDisable` is logged and the remaining hooks still run. Only singletons take part.

Constructors and `@PostConstruct` only wire objects. Work that needs the server belongs in
`onEnable`. Spring's own `@PostConstruct`, `@PreDestroy` and `DisposableBean` still apply.

On Paper, a bean that implements `SurfBootstrapLifecycle` or carries `@BootstrapComponent` lives in
the plugin's bootstrap context, built before the server exists. Phases per platform, the bootstrap
context, phase listeners and shutdown: `references/lifecycle.md`.

## surf-api's own beans

Every public surf-api type whose implementation is a root bean carries `@SurfApiBean`
(`dev.slne.surf.api.spring`), meta-annotated with `@Component` like `@PluginApi`. Component scans
never register an interface, so it creates no bean; it only lets the IDE treat the type as one when
only the API jar is on the classpath. Inject such a type; never look it up. A new public type whose
implementation is a root `@Service` gets the annotation too; `@InternalSurfApi` types do not.
Put it on interfaces only: the Kotlin Spring compiler plugin opens every class that carries
`@Component`, meta-annotations included, and its members with it, which breaks an abstract class
with explicit backing fields or `inline` members (`SpongeConfigSerializers` stays unannotated).

A third-party object that becomes usable only after the root refresh is a lazy root bean, such as
`LuckPerms` on Paper and Velocity; on Minestom `withLuckPerms()` registers it as a singleton after
the refresh. Inject such a bean as `ObjectProvider<T>` or `@Lazy`, or through a surf-api bean that
resolves it per call, such as `SurfLuckPerms`.

## Exporting a bean to other plugins

Every other bean stays private to its plugin. A plugin exports a bean by implementing an interface
annotated with `@PluginApi`; the implementing class keeps an ordinary stereotype:

```kotlin
import dev.slne.surf.api.spring.PluginApi
import org.springframework.stereotype.Service
import java.util.UUID

@PluginApi
interface EconomyApi {
    fun balance(player: UUID): Long
}

@Service
class EconomyService(private val accounts: AccountRepository) : EconomyApi {
    override fun balance(player: UUID) = accounts.balance(player)
}
```

A consumer injects `EconomyApi` once it declares the providing plugin as a direct dependency. On
Paper that is `surfPluginDependency("Economy")` in `surfPaperPluginApi { }`. The rules:

- The provider creates, initializes and destroys the instance; consumers only hold a reference. A
  provider disabled while consumers are open keeps its contexts open until the last consumer
  closes.
- An exported bean must not implement Spring's `Lifecycle` or `ApplicationListener`; such an
  export fails the provider's start.
- When two providers export the same type, a single-valued injection fails the consumer's start,
  also as `T?`. `@Primary`, `@Fallback` and load order never pick a provider; `@Qualifier` at the
  injection point does. `ObjectProvider.getIfUnique()` returns `null`.
- A plain `@Qualifier` value, `@Primary` and `@Fallback` travel with an export; custom qualifier
  annotations do not.

## Soft dependencies

Code that touches an optional plugin's API sits in an adapter bean carrying
`@ConditionalOnPlugin("Name")`. Several names in one annotation are alternatives for one plugin
whose name differs per platform; repeat the annotation to require several plugins. Spring reads
the condition from bytecode without loading the class, so a missing plugin never causes a
`NoClassDefFoundError`. The rest of the plugin injects
the adapter through an interface of its own as `T?` or `List<T>`. Detail:
`references/conditions.md`.

## What every plugin context provides

surf-api registers these beans in each plugin context before it refreshes:

| Type | Bean name | Where |
| --- | --- | --- |
| `CoroutineScope` | `surfCoroutineScope`, primary | every full plugin context; the plugin's own scope, cancelled after its disable hooks. The root holds surf-api's scope under the same name. |
| the plugin's main class, under every supertype | `surfPluginInstance`, primary | Paper, Velocity, Minestom |
| `ComponentLogger` (also an slf4j `Logger`) named after the plugin | `surfPluginLogger`, `@Fallback` | full plugin contexts on Paper, Velocity, Minestom; a `Logger` bean the plugin declares wins |
| `PaperDispatchers` (`main`, `async`, `region(...)`, `entity(...)`) | `surfPaperDispatchers`, primary | Paper |
| `org.bukkit.plugin.Plugin` proxy, `BootstrapContext` | `surfPlugin`, `surfBootstrapContext` | Paper bootstrap context |
| `ProxyServer`, `PluginContainer` | `surfProxyServer`, `surfPluginContainer` | Velocity |
| `PluginPresence` (`@InternalSurfApi`; read by `@ConditionalOnPlugin`, not meant for plugin code) | `surfPluginPresence` | every plugin context, bootstrap and full |
| `ConfigHandle<C>` per `@SurfConfig` class in the plugin's jar | `<class name>#configHandle` | every full plugin context; a plugin with `@SurfConfig` classes but no data folder fails to start |
| The proxy of every `@ReflectionProxy` interface in the plugin's packages; lazy | `<interface name>#reflectionProxy` | every full plugin context; the instance `reflectionProxy<T>()` returns, failing on first injection when the target class is missing |
| `RedisApi`, `RedissonClient`, `RedissonReactiveClient`; lazy, `@Fallback` | contributed by surf-redis-spring | full plugin contexts on Paper, Velocity, Minestom |
| `NatsClient` and a service proxy per generated service interface a bean asks for; lazy, `@Fallback` | contributed by surf-nats-spring | full plugin contexts on Paper, Velocity, Minestom |

The root context holds the server: `org.bukkit.Server` on Paper, `SurfMinestomServer` on Minestom.
surf-api registers no `Path` bean; a plugin reads its data folder from its platform.

Paper's bootstrap contexts receive no contributed beans, so a bootstrap component cannot inject
`ConfigHandle`, `RedisApi` or `NatsClient`.

The plugin instance is a managed singleton: the context injects its `@Autowired` members, runs the
`Aware` callbacks, `@PostConstruct` and every `BeanPostProcessor` on it, and calls `@PreDestroy`
and `DisposableBean.destroy` when it closes.

The `Plugin` of Paper's bootstrap context is a JDK proxy (`PluginInstanceHolder` in
`platform/paper/impl`), because Paper creates the instance only after the bootstrap context exists.
It forwards every call reflectively to the instance and throws an `IllegalStateException` before
the load phase. It compares equal to the instance, so listeners and tasks registered through it are
cleaned up with the plugin. Use it for registration and setup; code that runs per tick belongs in
the full plugin context, which injects the instance itself.

## IntelliJ IDEA

IntelliJ cannot see how surf-api wires beans and reports false positives.

- **Unused declarations.** `@SurfEventHandler`, `@NatsListener` and `@NatsService` members are
  called through reflection or generated invokers, so the inspection reports them as unused.
  Register the annotations as entry points: **Settings > Editor > Inspections > Unused declaration >
  Entry points > Annotations**, then add `dev.slne.surf.api.event.SurfEventHandler`,
  `dev.slne.surf.nats.event.NatsListener` and `dev.slne.surf.nats.NatsService`. Paper plugins add
  `dev.slne.surf.api.paper.packet.listener.listener.annotation.ClientboundListener` and
  `...ServerboundListener` as well. The list is stored in `.idea/misc.xml` (`EntryPointsManager`);
  an entry for a package that no longer exists, such as the pre-4.0
  `dev.slne.surf.api.core.event.SurfEventHandler`, matches nothing.
- **"Could not autowire. No beans of 'NatsClient' type found."** `NatsClient`, the NATS service
  proxies, `RedisApi` and `ConfigHandle` are registered at runtime by `ContextContributor`s, which
  call `registerBeanDefinition` on every plugin context. On the platforms the contributors are
  beans of the root context; in a microservice or another hosted application surf-api finds them
  through `ServiceLoader`. A contributor is not a component, a `@Bean` method or a Spring Boot
  auto-configuration, so IntelliJ's Spring model does not know these beans. The injection works at
  runtime; suppress the warning at the injection point or ignore it.

## Choosing between candidates

Needed once two beans of the plugin's own context provide the same type:

- `@Qualifier("name")` on the declaration names the bean; the same annotation on the parameter
  picks it. A bean's name also works as a qualifier.
- `@Primary` wins an unqualified injection point.
- `@Fallback` loses to every other candidate - the way to write a default that a real
  implementation replaces.
- `@Order` sorts collection injection and the lifecycle hooks; it never picks a single candidate.

Across providers of exports, only `@Qualifier` decides; see the export rules.

## The failures you will meet

Spring wraps the cause of a failed injection in `UnsatisfiedDependencyException`, and surf-api
wraps a failed plugin start in `PluginContextException` ("Failed to build the context of plugin
'<name>'"). Read the innermost cause.

| Failure | Means | Do |
| --- | --- | --- |
| `NoSuchBeanDefinitionException` | nothing provides the type in this context or its ancestors | annotate the implementation, publish it with `@Bean`, declare the providing plugin as a direct dependency, check that its interface carries `@PluginApi` and that no condition skipped it, or take `T?` |
| `NoUniqueBeanDefinitionException` | several candidates, nothing to choose between them | `@Primary` or `@Fallback` inside one context; `@Qualifier` at the injection point, which is the only choice across providers |
| `BeanCurrentlyInCreationException` | a constructor cycle | take one side as `ObjectProvider<T>` or `@Lazy`, or move the shared part into a third bean |
| `Plugin '<p>' exports bean '<b>', which implements Lifecycle or ApplicationListener.` | an export would be started or registered twice | move that part into a bean that implements no `@PluginApi` interface |
| `Plugin '<c>' imports several beans under the names [...]` | two different exports collide under one import name | fix the provider's bean names |
| `Plugin '<p>' is closed; its export '<b>' is no longer available` | a lookup reached an export after its provider closed | resolve the export while the provider runs |
| `'<p>' shares surf-api's class loader, and its packages [...] overlap dev.slne.surf.api` | a plugin on surf-api's class loader would be scanned by the root | move the plugin to a package of its own |
| `NoClassDefFoundError` for an optional plugin's class | a bean references a missing plugin's API | move the reference into a `@ConditionalOnPlugin` adapter |

## For surf-api modules

A surf-api module plugs a feature into every plugin context through a `ContextContributor` bean of
the root context (`dev.slne.surf.api.spring.context`, in `surf-api-spring-impl`). It runs after the
plugin's components are scanned and before the platform registers its own beans. A root that hosts
an application in the same context, such as a microservice, finds contributors through
`ServiceLoader`, so a contributor is also listed in `META-INF/services` and has a no-argument
constructor. `ContextPhaseListener` beans run after the enable hooks and before the disable hooks.
surf-api's static entry points read root beans lazily with the `@InternalSurfApi` delegate
`surfBean<T>()`. Code that runs on behalf of a plugin outside its hooks sets the plugin's class
loader as thread context class loader with the `@InternalSurfApi` `withContextClassLoader(loader) { }`
or the coroutine element `ContextClassLoaderElement(loader)`, both in
`dev.slne.surf.api.spring.context`.

## References

Read on demand:

- `references/injection.md` - every parameter shape, `@Bean` methods, qualifiers and their
  precedence, export resolution, and programmatic lookups.
- `references/lifecycle.md` - phases per platform, hook order, Paper's bootstrap context, phase
  listeners, coroutine scopes, the plugin instance and shutdown.
- `references/conditions.md` - `@ConditionalOnPlugin`, `PluginPresence` and Spring's own
  `@Conditional`.
