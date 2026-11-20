# Migrating to surf-api 4.0.0

This guide is for maintainers of code built on surf-api 3.x: Paper, Velocity and Minestom plugins,
Minestom servers, standalone programs, microservices and libraries such as surf-transaction and
surf-rabbitmq. Each section names a change and what it asks of your code or build. Section 21 sums
up the work as a checklist.

4.0.0 breaks binary and source compatibility. Recompile every plugin against it: a plugin compiled
against 3.x does not link.

surf-api 4.0 builds on Spring Framework: every plugin gets its own Spring application context, and
surf-api's services are beans of a root context above it. Sections 8, 9 and 15 to 17 cover what that
means for plugin code. For the design and the reasons behind it, see
[docs/spring-migration.md](docs/spring-migration.md).

surf-nats, surf-redis, surf-database-r2dbc and surf-microservice live in this repository under
`infra/` and release with surf-api; see section 13.

---

## 1. The group is unchanged

`dev.slne.surf.api`, as before. Only artifact ids and versions move.

## 2. Most consumers keep their coordinate

Every module was split into an `-api` part (contracts, what you compile against) and an `-impl`
part (what runs). Six **aggregate** artifacts publish under the names the old modules had and
export the parts:

| Keep writing | to get |
|---|---|
| `surf-api-core` | `surf-api-core-api` and `surf-api-config-api` |
| `surf-api-paper` | `surf-api-paper-api` |
| `surf-api-minestom` | `surf-api-minestom-api` |
| `surf-api-velocity` | `surf-api-velocity-api` |
| `surf-api-minestom-server` | `surf-api-minestom-impl` |
| `surf-api-standalone` | `surf-api-standalone-impl` |

If your build file names one of these six, it needs no change for the rename.

The four `-api` aggregates export only the api part. Code that loads an implementation class at
runtime, by reflection or in a test, needs the matching `-impl` coordinate on its classpath.

## 3. Coordinates that changed and are **not** aliased

These were renamed with no relocation POM behind them. A build file naming one of these fails to
resolve with "not found" and no hint at the new name.

| Old artifact | New artifact |
|---|---|
| `surf-api-core-server` | `surf-api-core-impl` |
| `surf-api-paper-server` | `surf-api-paper-impl` |
| `surf-api-velocity-server` | `surf-api-velocity-impl` |
| `surf-api-paper-nms-common` | `surf-api-paper-nms-api` |
| `surf-api-shared-public` | `surf-api-annotations` |
| `surf-api-shared-internal` | `surf-api-internal` |

If you depended on any of these directly, you were depending on an implementation module. Prefer
the matching aggregate from section 2, and use an `-impl` coordinate only where you need the
implementation itself.

## 4. The Minestom feature artifacts were renamed, with aliases

The eight Minestom server features moved from `surf-api-minestom-server-<feature>` to
`surf-api-minestom-<feature>-impl`, and each publishes a relocation POM, so an old coordinate
still resolves and Gradle tells you the new name:

`bootstrap`, `console`, `luckperms`, `npc`, `player-visibility`, `plugins`, `signed-chat`,
`spark`.

You do not normally name these at all: the `withXxx()` calls in the Gradle block
`surfMinestomServerApi { }` put the features you ask for on the class path.

## 5. Added artifacts

| Artifact | What it is |
|---|---|
| `surf-api-registry-keys` | The four generated key classes (block types, item types, sounds, advancements), 40 848 lines that used to sit inside `surf-api-core` and only change with the Minecraft version. A separate module, so a core change does not recompile them. |
| `surf-api-spring-api` | The contracts a plugin writes its beans against: `SurfLifecycle`, `SurfBootstrapLifecycle`, `@PluginApi`, `@BootstrapComponent` and `@ConditionalOnPlugin`. Exports `spring-context`. |
| `surf-api-spring-impl` | The context manager behind the root, import and plugin contexts. |
| `surf-api-standalone-testing` | Starts surf-api's standalone runtime once per test JVM; see section 13. |
| `surf-api-bom` | A platform BOM listing every published artifact at one version. Prefer importing it over pinning versions one by one. |
| `surf-api-inventory-api` | The platform-neutral half of the inventory framework: view settings, header geometry, text alignment, the container component base and the DSL markers. Paper and Minestom each carried their own copy; see section 12. |
| `surf-api-common-api` | The shared base `surf-api-core` grew around: `util` (including `logger`), `collection`, `math`, `random`, `font`. Nothing in it names anything else of surf-api's, so every other domain can depend on it. |
| `surf-api-serialization-api` | The kotlinx-serialization serializers: `SurfSerializerModule`, the Adventure serializers, the JDK ones (UUID, dates, IP, numbers) and the SpongePowered math ones. |
| `surf-api-config-api` | The configuration surface: `SurfConfigApi`, `@SurfConfig` and `ConfigHandle`, the `SpongeYmlConfigClass` / `SpongeJsonConfigClass` companions, the 23 constraint annotations, the helper types, the managers and the migrations. |
| `surf-api-config-impl` | The config service and the `ConfigHandle` registration for plugin contexts. |
| `surf-api-messages-api` | The text surface: `Colors`, `CommonComponents`, `ComponentMessage`, the `buildText` / `sendText` builders, the colour palettes, the message bundles, pagination, the configured MiniMessage instance, the action bar and `Rarity`. |
| `surf-api-event-api` | `SurfEventBus` and the event contracts: `SurfEvent`, `SurfSyncEvent`, `SurfAsyncEvent`, `SurfCancellableEvent`, `@SurfEventHandler`, `SurfEventPriority`. |
| `surf-api-nbt-api` | The Adventure binary-tag surface: `CollectionBinaryTag`, `FastCompoundBinaryTag` and `InternalNbtBridge`. |
| `surf-api-reflection-api` | `SurfReflection` and the `@SurfProxy` annotations. Separate from nbt: they share no import, and a consumer of the NMS layer should not need a tag module to get a dynamic proxy. |
| `surf-api-command-api` | What all three platforms' command layers share: `SurfCommandUtil`, `CommandExceptionBuilder`, `WrapperCommandExceptionComponent`, `SuspendArgumentHelper` and `CommandRegistrar`. A module of its own, so that a plugin pulling in the colour palette does not also get CommandAPI on its compile classpath. |
| `surf-api-luckperms-api` | The LuckPerms lookup helpers. A module of its own for the same reason. |

**You do not name these in a build file, except `surf-api-standalone-testing`.** Every coordinate
that carried these types before re-exports them, so `surf-api-core`, `surf-api-paper`,
`surf-api-minestom`, `surf-api-velocity`, `surf-api-standalone` and `surf-api-minestom-server` all
resolve them unchanged. What *did* change is which package a type lives in; see section 6.

## 6. `surf-api-core` was cut into domains, so some packages moved

The artifacts are in section 5 and need no build-file change. The package prefixes do:

| Old package | New package |
|---|---|
| `dev.slne.surf.api.core.util.…` | `dev.slne.surf.api.common.util.…` |
| `dev.slne.surf.api.core.collection.…` | `dev.slne.surf.api.common.collection.…` |
| `dev.slne.surf.api.core.math.…` | `dev.slne.surf.api.common.math.…` |
| `dev.slne.surf.api.core.random.…` | `dev.slne.surf.api.common.random.…` |
| `dev.slne.surf.api.core.font.…` | `dev.slne.surf.api.common.font.…` |
| `dev.slne.surf.api.core.serializer.…` | `dev.slne.surf.api.serialization.…` |
| `dev.slne.surf.api.core.messages.…` | `dev.slne.surf.api.messages.…` |
| `dev.slne.surf.api.core.minimessage.…` | `dev.slne.surf.api.messages.minimessage.…` |
| `dev.slne.surf.api.core.actionbar.…` | `dev.slne.surf.api.messages.actionbar.…` |
| `dev.slne.surf.api.core.config.…` | `dev.slne.surf.api.config.…` |
| `dev.slne.surf.api.core.server.config.…` | `dev.slne.surf.api.config.impl.…` |
| `dev.slne.surf.api.core.event.…` | `dev.slne.surf.api.event.…` |
| `dev.slne.surf.api.core.nbt.…` | `dev.slne.surf.api.nbt.…` |
| `dev.slne.surf.api.core.reflection.…` | `dev.slne.surf.api.reflection.…` |
| `dev.slne.surf.api.core.command.…` | `dev.slne.surf.api.command.…` |
| `dev.slne.surf.api.core.luckperms.…` | `dev.slne.surf.api.luckperms.…` |
| `dev.slne.surf.api.core.rarity.Rarity` | `dev.slne.surf.api.messages.rarity.Rarity` |
| `dev.slne.surf.api.core.algorithms.…` | `dev.slne.surf.api.common.algorithms.…` |
| `dev.slne.surf.api.core.invoker.…` | `dev.slne.surf.api.common.invoker.…` |
| `dev.slne.surf.api.core.environment.…` | `dev.slne.surf.api.config.environment.…` |
| `dev.slne.surf.api.core.inventory.framework.internal.…` | `dev.slne.surf.api.inventory.framework.internal.…` |

The subpackage and every type name are unchanged; only the prefix is. The two that account for
most of the edits are `logger()`, at `dev.slne.surf.api.common.util.logger`, and the text builders
`buildText` / `sendText` with `Colors`, under `dev.slne.surf.api.messages.adventure` and
`dev.slne.surf.api.messages`.

**Two types are not a prefix rewrite:**

- `TransformingObjectSet` declared `dev.slne.surf.api.core.api.collection` while sitting in a
  `collection/` directory. It is `dev.slne.surf.api.common.collection.TransformingObjectSet`.
- `dev.slne.surf.api.core.serializer.NbtOps` was removed together with DataFixerUpper; see
  section 19.

**One move changes what an aggregate resolves.** The eight `framework.internal` files went
*downstream*, into `surf-api-inventory-api`, so `surf-api-core`, `surf-api-velocity` and
`surf-api-standalone` no longer carry them. `surf-api-paper`, `surf-api-minestom` and
`surf-api-minestom-server` still do. Every public declaration in them carries `@InternalSurfApi`.
`DefaultFontWidths`, `ShiftGlyphs` and `TextAlignmentMath` are pure Adventure text measurement, so a
Velocity or standalone plugin that opted in could call `DefaultFontWidths.textWidth` or
`appendShiftedComponent`; such a call no longer resolves.

There are no deprecated typealiases in the old packages: an import that names one fails to resolve.
The compiler lists every one of them, so the migration is "compile, follow the unresolved
references", and it touches only `import` lines, never a build file.

## 7. fastutil is gone

**This is the change most likely to touch your code.**

surf-api no longer uses, exposes or exports [fastutil]. Its public signatures use the JDK
collection types, and the helper bridge `fast-util-util.kt`, with 1 096 functions such as
`toObjectList` and `toInt2ObjectMap`, was deleted.

fastutil is not on your compile classpath through surf-api or the surf Gradle plugins, so no
repository picks it up by accident.

What to do:

| If your code has | Write instead |
|---|---|
| `ObjectArrayList<T>`, `ObjectList<T>` | `ArrayList<T>`, `List<T>` |
| `Object2ObjectOpenHashMap`, `Object2ObjectMap` | `HashMap`, `Map` |
| `ObjectOpenHashSet`, `ObjectSet` | `HashSet`, `Set` |
| `Int2ObjectOpenHashMap` | `HashMap<Int, V>` |
| `IntArrayList`, `IntList`, `IntSet` | `ArrayList<Int>`, `List<Int>`, `Set<Int>` |
| `list.toObjectList()`, `map.toObject2ObjectMap()`, and the other helpers | nothing: delete the call, the receiver already is the type you want |

If you have measured a hot path that needs a primitive-specialised collection, declare
`it.unimi.dsi:fastutil` in that module yourself. It is not inherited from surf-api.

### The one feature that is removed, not translated

`SpongeConfigSerializers` registered Configurate `TypeSerializer`s for **39 fastutil map types**.
Those registrations are gone. A `@ConfigSerializable` class with a field typed as a fastutil map
is no longer serialisable and fails on load. Change such a field to the JDK map, `Map<K, V>`, which
Configurate handles.

### Where fastutil stays

Five modules keep it on the **compile** classpath only, and it reaches no published artifact:

- the four `surf-api-paper-nms-v*` modules, for `CreativeOrderComparator`: one map is keyed
  through Paper's own `ItemStackLinkedSet.TYPE_AND_TAG` hash strategy and one through reference
  identity, and the JDK has no equivalent for either.
- `surf-api-minestom-impl`, because a mixin `@Shadow` of a Minestom field typed
  `Int2ObjectSyncMap` has to name that exact type to match.
- `surf-api-paper-nms-v26-1`, `-v26-2` and `-v26-3` for a second reason: `@ReflectedField` looks
  a field up through a `VarHandle`, which matches on the field's *exact declared type*. The server holds
  `LastSeenMessagesValidator.trackedMessages` as `ObjectList`, so the accessor declares that type;
  `MutableList` would make the lookup throw `NoSuchFieldException` in a static initialiser and stop
  surf-api from starting. The NMS version modules are implementation modules with no ABI dump, so
  the type reaches no published artifact.

[fastutil]: https://fastutil.di.unimi.it/

## 8. Service lookups were replaced by Spring beans

The service-loader lookup `requiredService<T>()` is gone. surf-api's services are beans of
surf-api's **root** Spring context, and your plugin's beans receive them by constructor injection.
Every injectable surf-api type carries `@SurfApiBean`, which is meta-annotated with `@Component`, so
the IDE knows it as a bean and suggests it as a constructor parameter.

### The static accessors are internal

In 3.x, many api interfaces declared `companion object : Foo by service`, so `Foo.doThing()` and
`Foo.INSTANCE` reached the service from anywhere. In 4.0 these companions either no longer exist
or carry an `INSTANCE` or `instance` property annotated `@InternalSurfApi`. That annotation is an
opt-in at error level: plugin code that reads the property does not compile. `@InternalSurfApi`
declarations may change in any release. `@PublishedSurfApi` marks the few internal declarations
that surf-api's public `inline` functions call; plugins must not call them either, but they stay
binary compatible, so plugins compiled against those inline functions keep working. Declare the interface
as a constructor parameter of a bean instead:

```kotlin
@Service
class Highlighter(private val glowing: SurfGlowingApi) {
    fun highlight(target: Entity, viewer: Player) = glowing.makeGlowing(target, viewer)
}
```

Code that is not a bean, such as an `object` or a top-level function, gets the service from a bean
that injects it, for example as a parameter.

The following table lists the 3.x accessors and what to inject instead. Each injected type is a bean
of surf-api's root context and resolves in every plugin context. Paper's bootstrap context is the
exception: it sees only surf-api's bootstrap components (section 15).

| 3.x accessor | 4.0 |
|---|---|
| `SurfApiCore.instance`, `SurfApiCore.getPlayer(…)` and the other delegated calls | Inject `SurfApiCore`, or the platform type `SurfApiPaper` or `SurfApiVelocity`. |
| `SurfApiPaper.INSTANCE`, `SurfApiVelocity.INSTANCE` | Inject `SurfApiPaper` or `SurfApiVelocity`. |
| `SurfConfigApi.instance`, `surfConfigApi` | Inject `SurfConfigApi`, or declare the file with `@SurfConfig` (section 17). `SpongeYmlConfigClass` and `SpongeJsonConfigClass` companions keep working. |
| `surfSpongeConfigSerializers` | Removed. Register serializers from a `ConfigSerializerRegistrar` bean (section 17). |
| `SurfEventBus.INSTANCE`, `SurfEventBus.registerListeners(…)` and the other delegated calls | Inject `SurfEventBus`, or annotate methods of a bean with `@SurfEventHandler` (section 17). `call()` on an event is unchanged. |
| `SurfReflection.INSTANCE`, `surfReflection` | Inject `SurfReflection`; `createProxy<T>()` is an extension on it. |
| `ActionBarService.instance` | Removed. Call the `Audience.sendActionBar(…)` extension, or inject `ActionBarService`. |
| `InternalPaginationBridge.INSTANCE`, `InternalNbtBridge.INSTANCE` | Internal to surf-api; no replacement. |
| Paper: `SurfGlowingApi`, `SurfPaperPacketApi`, `SurfPaperPacketListenerApi`, `SurfPaperVisualizerApi`, `TickThreadGuard`, `SurfPaperPAPIHook`, `SuspendRequirementService` | Inject the interface. |
| Paper: `BlockPdcProvider.INSTANCE` | Call the `Block.pdc()` extension, or inject `BlockPdcProvider`. |
| Paper NMS: `SurfPaperNmsBridge`, `SurfPaperNmsCommonBridge`, `SurfPaperNmsCommandArgumentTypesBridge`, `SurfPaperNmsEntityBridge`, `SurfPaperNmsGlowingBridge`, `SurfPaperNmsItemBridge`, `SurfPaperNmsLootTableBridge`, `SurfPaperNmsNbtBridge`, `SurfPaperNmsPlayerBridge`, `SurfPaperNmsPortalBridge`, `SurfPaperNmsStatsBridge`, `SurfPaperNmsPacketBridges`, `SurfPaperNmsBlockPackets`, `SurfPaperNmsSpawnPackets`, `SurfPaperNmsPlayerChatPackets`, `SurfPaperNmsPlayerPackets`, `SurfPaperNmsPlayerToastPackets` | Inject the interface. |
| Paper: `SurfSidebarFactory.INSTANCE` | Call `sidebar(plugin) { }`, `autoUpdatingSidebar(plugin) { }` or `globalSidebar(plugin) { }`. |
| Paper: `SurfScoreboardApi.INSTANCE` | Removed with the scoreboard API; see section 18. |
| Paper and Minestom: `ViewFrameAccessor.INSTANCE`, the top-level `viewFrame` | Inject `ViewFrame` (`me.devnatan.inventoryframework.ViewFrame`) or `ViewFrameAccessor`. The extension functions of the inventory framework, such as `register()`, keep working. |
| `SurfComponentApi`, `surfComponentApi` | Removed; see section 9. |

`PluginManager.instance` on Minestom stays public.

### What else changed

- **`@AutoService` does not publish an implementation to surf-api.** surf-api asks Spring, not
  `ServiceLoader`. If you registered an implementation of a surf-api interface through
  `@AutoService` and expected surf-api to find it, make it a bean instead.
- **A service is available once surf-api's root context runs.** surf-api builds the root context
  before any plugin loads. Code that runs during surf-api's own class initialisation, before the
  root context exists, gets an `IllegalStateException` saying that surf-api's root context has not
  been started yet.
- **Bean metadata is not generated at build time.** The KSP processor of the surf Gradle plugins
  writes no `META-INF/surf-api/beans/` index. Spring finds a plugin's beans by scanning its jar;
  see section 15.

## 9. The component framework was replaced by Spring beans

**If your plugin has classes implementing `SurfComponent`, this is the change that costs the most
work.** The 3.x component framework is gone, and a plugin's classes are Spring beans. Use Spring's
own annotations:

| 3.x | 4.0 |
|---|---|
| `dev.slne.surf.api.shared.api.component.types.Service`, `.Repository` | `org.springframework.stereotype.Service`, `.Repository` (or `@Component`) |
| `…shared.api.component.SurfComponentMeta` | Meta-annotate your own annotation with `@Service` or `@Component`. |
| `…shared.api.component.SurfComponent` with `load`, `enable`, `disable` | `dev.slne.surf.api.spring.SurfLifecycle` with `onLoad`, `onEnable`, `onDisable`; on Paper also `SurfBootstrapLifecycle.onBootstrap` |
| `…shared.api.component.Priority` | `@Order` for the order of the lifecycle hooks; `@Primary`, `@Fallback` or `@Qualifier` to choose between candidates for injection |
| `…shared.api.component.processor.ComponentPostProcessor` | Spring's `BeanPostProcessor` |
| `…shared.api.component.condition.ComponentCondition` and `@ConditionalOn` | Spring's `Condition` with `@Conditional` |
| `ConditionalOnEnvironment`, `ConditionalOnProductionEnvironment`, `ConditionalOnDevelopmentEnvironment` | Spring's `@Profile`. surf-api activates no profile and does not read `SURF_ENVIRONMENT`; set `-Dspring.profiles.active=production` or `SPRING_PROFILES_ACTIVE` on the server JVM. |
| `ConditionalOnProperty` | A `Condition` that reads `context.environment`; see the example after this table. |
| `DependsOnPlugin` | `@ConditionalOnPlugin` from `dev.slne.surf.api.spring`. Repeat it once per required plugin; several names in one annotation are alternatives. |
| `DependsOnOnePlugin` | One `@ConditionalOnPlugin` with every name: it holds when any of them is installed. |
| `DependsOnClass`, `DependsOnClassName` | A `Condition` of your own, for example with Spring's `ClassUtils.isPresent`. |
| `DependsOnComponent` | A constructor parameter. A required parameter fails the start when no bean provides it; a nullable one (`T?`) or `ObjectProvider<T>` makes it optional. |
| `ConditionalOnMissingComponent` | `@Fallback` on the default bean, or `@Primary` on the bean that should win |
| `SurfComponentApi`, `surfComponentApi`, `AbstractComponent`, `ComponentService` | Nothing. surf-api builds and drives the plugin's context itself. |
| `surfComponentApi.load(this)`, `.enable(this)`, `.disable(this)` | Delete the calls. The platform base class of the main class drives the plugin's context; see section 15. |

The plugin contexts on Paper, Velocity and Minestom run Spring Framework, not Spring Boot, so Boot's
`@ConditionalOnProperty`, `@ConditionalOnClass` and `@ConditionalOnMissingBean` are not available
there. A property condition looks like this:

```kotlin
class ShopFeatureEnabled : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) =
        context.environment.getProperty("shop.feature") == "true"
}

@Service
@Conditional(ShopFeatureEnabled::class)
class ShopFeature : SurfLifecycle
```

A bean that uses the API of an optional plugin goes into an adapter guarded by
`@ConditionalOnPlugin`. Spring evaluates the condition without loading the class, so the adapter's
reference to the missing plugin never fails. The rest of the plugin injects the adapter's
interface as a nullable type:

```kotlin
interface Placeholders {
    fun register()
}

@Service
@ConditionalOnPlugin("PlaceholderAPI")
class PlaceholderApiPlaceholders : Placeholders {
    override fun register() { /* calls PlaceholderAPI */ }
}

@Service
class Hud(private val placeholders: Placeholders?)
```

Three differences are not renames and need a decision:

- **A bean declares its dependencies in its constructor.** A component that reached for a
  collaborator through a service lookup takes it as a constructor parameter instead, and Spring
  orders the graph. For the parameter shapes, see `.claude/skills/surf-api-components/`.
- **The phases do not chain.** `AbstractComponent.enable()` ran `load()` first if it had not run.
  surf-api runs every bean's `onLoad`, then every bean's `onEnable`, each in the order in which the
  beans finished initialising, which puts a bean's dependencies first. `onDisable` runs in reverse.
  Move work that relied on the chaining into the phase it belongs to.
- **Hooks block the platform thread.** On Paper, `onLoad`, `onEnable` and `onDisable` run on the
  main thread and block it, so a hook must not wait for work dispatched to the main thread, a
  region thread or the Bukkit scheduler. Constructors and `@PostConstruct` only wire objects.

A plugin's beans see surf-api's beans and the beans its direct surf-plugin dependencies export;
surf-api never sees the plugin's beans. To export a bean to other plugins, implement an interface
annotated with `@PluginApi` in the plugin's api module; see section 15. `local/paper-plugin-test`
is a working example, conditions included: `PaperPluginMain.kt`, `TestPluginLifecycle.kt` and the
`hook` package.

## 10. `runServer` was removed

The `runServer` task is gone, along with the `xyz.jpenilla.run-paper` plugin that brought a run
task in and the `runDirectory` property of the Gradle block `surfMinestomServerApi { }`. There is
no replacement task.

Start a server the way the manifest requires:

```
./gradlew shadowJar
java -jar build/libs/NAME-all.jar
```

Replace `NAME` with the base name of your server's shadow jar.

`java -jar` on the shaded jar is required. A Minestom server's agents are declared in the jar
manifest, and a manifest agent only applies when the JVM starts on that jar: launching the main
class off a classpath skips the mixin transformer with no error. If you want a task, write one in
your own build; it has to be an `Exec`, not a `JavaExec`, for the same reason.

## 11. Hidden binary-compatibility declarations were removed

The inventory framework carried `@Deprecated(level = DeprecationLevel.HIDDEN)` overloads and one
hidden class (`ViewContainerBackHintComponent`); `PacketOperation.kt` carried a hidden non-inline
`PacketOperation(block)`, which is why the inline one was named `PacketOperationInline` on the JVM
and is simply `PacketOperation` in 4.0. They existed only so that plugins compiled against an older
surf-api still linked. No plugin compiled against 3.x links against 4.0.0, so they are removed.

If you compile against 4.0.0 you will not notice: a hidden declaration was never visible from
source.

## 12. The platform-neutral half of the inventory framework moved packages

Paper and Minestom each carried a full copy of the inventory framework, 61 files per platform.
21 of those files name no platform type, so they live once in `surf-api-inventory-api` under a
package of their own:

| Old package | New package |
|---|---|
| `dev.slne.surf.api.paper.inventory.framework.…` | `dev.slne.surf.api.inventory.framework.…` |
| `dev.slne.surf.api.minestom.inventory.framework.…` | `dev.slne.surf.api.inventory.framework.…` |

The subpackage and the type names are unchanged; only the prefix is, and the platform segment is
gone. These are the declarations that moved:

- `framework.dsl`: the `IFContext` / `IFConfinedContext` / `IFOpenContext` / `IFCloseContext` /
  `IFSlotClickContext` extensions (`cancel`, `openForPlayer`, `openForEveryone`), `LayoutBuilder`
  and `ViewConfigBuilder.layout`
- `framework.view`: `InventoryFrameworkDSL`
- `framework.view.container.component`: `ViewContainerComponent`
- `framework.view.container.component.components`: `ViewBlockCellComponent`
- `framework.view.icon`: `ViewIconColor`, `ViewIconType`
- `framework.view.pagination`: `PaginationPageIndicator`
- `framework.view.settings`: `ViewRows`, `ViewFontMetrics`, `ViewHeaderGeometry`,
  `PaginationViewRows`, `PaginationEmptyRows`, `PaginationButtonPosition`
- `framework.view.settings.align`: `TextAlignment`, `TextAlignmentOptions`
- `framework.view.util`: `GlyphShift`, `shift`

**There are no deprecated typealiases in the old packages.** An import of one of the names above
from `dev.slne.surf.api.paper.inventory` or `dev.slne.surf.api.minestom.inventory` fails to
resolve; change the import. `surf-api-paper` and `surf-api-minestom` still export every one of these
types, so you edit `import` lines, never your build file.

Everything else stayed where it was, per platform, because it names a platform type: the views
themselves (`AbstractSurfView`, `AbstractPaginatedSurfView`, the DSL builders and contexts), the
buttons, `ViewIcon`, `ViewFrameAccessor`, the state helpers, the container and its DSL, and the
three settings types (`SurfViewSettings`, `SimpleViewSettings`, `PaginatedViewSettings`) with their
defaults and builders. Those keep their `…paper.inventory…` and `…minestom.inventory…` packages, so
an import of `AbstractSurfView` or `surfView` does not change.

## 13. surf-nats, surf-redis, surf-database-r2dbc and surf-microservice moved into this repository

The four libraries live under `infra/`, with their history, and release with surf-api: one
group, one version, one BOM. Their Kotlin packages did not change, so no `import` line does. What
changed are the names a build file or a server sees. Section 14 covers the microservice types that
were removed, which no rename fixes.

**Coordinates.** Every artifact moved to the group `dev.slne.surf.api`, and its name gained the
`surf-api-` prefix. None of the old coordinates is aliased.

| Old coordinate | New coordinate |
|---|---|
| `dev.slne.surf.nats:surf-nats-<module>` | `dev.slne.surf.api:surf-api-nats-<module>` |
| `dev.slne.surf.redis:surf-redis-<module>` | `dev.slne.surf.api:surf-api-redis-<module>` |
| `dev.slne.surf.microservice:surf-microservice-<module>` | `dev.slne.surf.api:surf-api-microservice-<module>` |
| `dev.slne.surf:surf-database-r2dbc` | `dev.slne.surf.api:surf-api-database-r2dbc` |
| npm `@slne/surf-nats` | npm `@slne/surf-api-nats` |

**`withSurfRedis()`.** The overload `withSurfRedis(version, relocation)` of the Paper, Velocity,
Minestom and Minestom server plugins is removed; call `withSurfRedis()`. It adds
`dev.slne.surf.api:surf-api-redis-api` at the surf-api version as a `compileOnly` dependency.
surf-redis is not shaded into plugins and not relocated, also not when it comes in as a project
dependency: surf-api ships the Redis integration.

**`withSurfDatabaseR2dbc` is removed.** The `withSurfDatabaseR2dbc(version, relocation)` call of the
Paper, Velocity, Minestom and Minestom server plugins is gone, and so is the relocation of
`dev.slne.surf.database` that it set up, including the one applied for a project dependency. The
database library serves microservices only. A plugin or server that used it moves the database logic
into a microservice, which gets `surf-api-database-r2dbc` from the
`dev.slne.surf.api.gradle.microservice` plugin (section 14).

**Gradle plugins.** The surf-nats and surf-microservice plugins are part of
`surf-api-gradle-plugin` and carry its version.

| Old plugin id | New plugin id |
|---|---|
| `dev.slne.surf.nats` | `dev.slne.surf.api.gradle.nats` |
| `dev.slne.surf.microservice` | `dev.slne.surf.api.gradle.microservice` |

**Redis and NATS platform plugins.** The `surf-redis-<platform>` and `surf-nats-<platform>`
plugins are gone: surf-api bundles both integrations on Paper, Velocity and Minestom. This changes
three things on a running server:

- Plugins declare no dependency on them. Remove `surf-redis-paper`, `surf-api-redis-paper` and
  their NATS, Velocity and Minestom counterparts from `depend`, `softdepend`, `paper-plugin.yml`
  dependencies, `velocity-plugin.json` and `minestom-plugin.json`. A plugin that still lists one
  fails to load because the plugin does not exist any more.
- The configuration lives in surf-api's data folder, in `redis/config.yml` and `nats/config.yml`.
  That folder is `plugins/surf-paper-api/` on Paper, `plugins/surf-api-velocity/` on Velocity and
  `surf-api` inside the plugins directory on Minestom (`plugins/surf-api/` unless the
  `surf.minestom.plugins` system property moves it). On the first start surf-api copies the file of
  the old plugin folder (`plugins/surf-redis-paper/`, `plugins/surf-api-redis-paper/` and the Velocity
  and Minestom equivalents, for NATS as well) when the new file does not exist. The old file stays
  in place.
- Delete the old `surf-redis-*` and `surf-nats-*` plugin jars from `plugins/`. A leftover jar loads a
  second copy of the integration next to the bundled one.

A plugin receives Redis and NATS as beans of its own context: a `RedisApi`, `RedissonClient`,
`NatsClient` or generated NATS service proxy is injected like any other bean. For the rules, see the
Redis and NATS sections of [docs/spring-migration.md](docs/spring-migration.md).

**surf-redis without fastutil.** surf-redis follows section 7: its public signatures return JDK
collections (`SyncList.snapshot()` returns `ArrayList`, `SyncMap.snapshot()` a `HashMap`,
`SyncSet.snapshot()` a `HashSet`, and the `ByteBuf` list and map codecs read and write `ArrayList`,
`List<Int>` and `HashMap`). A caller that stored a result in a fastutil-typed variable changes the
type; the wire format is unchanged.

**surf-redis without messaging.** The Pub/Sub event bus and the request/response bus are gone;
surf-nats replaces both. Removed are the packages `dev.slne.surf.redis.event` and
`dev.slne.surf.redis.request` and the `RedisApi` members `eventBus`, `requestResponseBus`,
`publishEvent`, `subscribeToEvents`, `registerEventCodec`, `registerEventType`, `sendRequest` and
`registerRequestHandler`, along with the Reactor helpers `Mono.asDeferred()` and the `Tuple2`
destructuring operators from `dev.slne.surf.redis.util`. The surf-nats README maps each of them to
its replacement under "Migrating from surf-redis and surf-rabbitmq". Sync structures and caches are
unchanged.

**Tests that use surf-api beans.** surf-api's beans, and the API that reads them such as config
classes, work only while surf-api's root context runs, which a plain JUnit test never starts. Add
`surf-api-standalone-testing` to the test runtime:

```kotlin
testRuntimeOnly("dev.slne.surf.api:surf-api-standalone-testing:VERSION")
```

Replace `VERSION` with the surf-api version. The module registers `SurfApiStandaloneTestSession`, a
JUnit Platform `LauncherSessionListener` that starts surf-api's standalone runtime once per test JVM
and shuts it down after the last test.

## 14. Microservices became Spring Boot applications

A surf microservice is a Spring Boot application. The `Microservice` base class, its launcher and
its integration SPI are gone, together with `DatabaseApi`. Section 13 only renames coordinates. The
types below were removed, so code that used them does not compile. The runtime and the other
modules are documented in the surf-microservice README.

**Entry point.** Replace the `Microservice` subclass with a `@SpringBootApplication` class and a
`main` function. The function needs a block body, because `runMicroservice` returns `Nothing`:

```kotlin
@SpringBootApplication
class PunishApplication

fun main(args: Array<String>) {
    runMicroservice<PunishApplication>(args)
}
```

The Gradle plugin finds the `main` function itself. Remove `@AutoService(Microservice::class)` and
the KSP processor `surf-microservice-processor`, which listed the main class and the migrations.

**Removed types and their replacements.**

| Removed | Replacement |
|---|---|
| `Microservice` | A `@SpringBootApplication` class and `runMicroservice<App>(args)` |
| `Microservice.name`, `Microservice.dataPath` overrides | The `MicroserviceInfo` bean. The name comes from `SURF_MICROSERVICE_NAME`, then `spring.application.name`, then the class name without an `Application` or `Microservice` suffix. The directory comes from `SURF_MICROSERVICE_CONFIG_DIR`, default `config`, and is also the property `surf.config-directory`. |
| `onBootstrap(args)` | `onLoad` or `onEnable` of a `SurfLifecycle` bean. Open resources in the constructors and `@Bean` methods of the beans that own them. The arguments are Spring Boot's `ApplicationArguments`. |
| `onDisable()` | `onDisable` of a `SurfLifecycle` bean, or `@PreDestroy` and `destroyMethod` of the bean that owns the resource |
| `DatabaseApi`, `DatabaseApi.create`, `TestOnlyDatabaseApi`, `Microservice.database` | The injected `R2dbcDatabase` and `SurfMigrationRunner` beans. The Spring context opens and closes the pool. |
| `migrate(DatabaseApi, …)`, `migrateReliabilitySchema` | `SurfMigrationRunner.migrate(…)` and the `ReliabilityMigrationSet` bean |
| `generateMigrationScript(DatabaseApi, …)` | `R2dbcDatabase.generateMigrationScript(…)` |
| `AtomicOperations(DatabaseApi, …)` | The injected `AtomicOperations` bean, or `AtomicOperations(R2dbcDatabase, …)` |
| `DatabaseCustomizer.configureDatabase` | A `SurfDatabaseConfigurer` bean |
| `DatabaseCustomizer.configureMigrations` | A `MigrationSet` bean |
| `DatabaseCustomizer.runMigrations = false` | The property `surf.database.migrations.enabled=false`, only for a service that ships migrations and does not want them to run |
| `ListedMigrations` and the KSP processor | Convention. SQL in `db/migration` (or `db/migration/<vendor>`) and every `SurfMigration` below the package of the `@SpringBootApplication` class run in the set of the service. |
| `registerTransactionalService<T>(impl)` | `@NatsTransactional` on the `@NatsService` bean |
| `Microservice.events` | The injected `TransactionalEvents` bean |
| `TransactionalHandlerFixture.create(databaseApi)` | `TransactionalHandlerFixture.create(database, migrationRunner)` |
| `ReliabilityCustomizer` | A `ReliabilityConfig` bean |
| `MicroserviceIntegration`, `MicroserviceIntegrationContext` and the order constants | Ordinary beans. A resource that must start or stop at a fixed point is a `SmartLifecycle` bean with a phase: below `0` before the surf-api hooks, above `0` after them. |
| `MicroserviceHealthContributor`, `MicroserviceHealthCheckResult`, `MicroserviceHealthStatus` | A Spring Boot `HealthIndicator` bean. The bean name without the `HealthIndicator` suffix is the component name. |
| `Microservice.commands`, command registration in `onBootstrap` | `MicroserviceCommand` beans, or `MicroserviceCommands.register` and `microserviceCommand` at any time |
| `surf-nats-standalone` and `SurfNatsStandalone` in a microservice | The NATS beans of the Spring context. Remove your own start and shutdown calls. |

**Behavior that changed.**

- The context creates the NATS client and connects it. Delete your own `NatsClient.create(...)`.
- The database opens and migrates while the context starts, before the surf-api hooks, so a
  schema fix in `onEnable` comes too late for beans that already injected `R2dbcDatabase`.
- A `@NatsService`, `@NatsTransactional` or `@NatsListener` bean is always created eagerly, even
  with `spring.main.lazy-initialization=true`. A bean of these kinds that is first created after
  the application was enabled fails.
- A `@NatsTransactional` bean fails the start unless exactly one `NatsServiceInterceptorProvider`
  answers for it and declares `handlesTransactional`. A custom provider that handled
  `@NatsTransactional` before has to override `handlesTransactional` or step aside.
- A `DatabaseConfig` bean of the application is validated and used as it is, without the
  `SURF_DATABASE_*` overrides. `SURF_CONFIG_DIRECTORY` has no effect in a microservice: use
  `SURF_MICROSERVICE_CONFIG_DIR`.
- Exposed's implicit retries are off. `SurfDatabaseAutoConfiguration` sets `defaultMaxAttempts` to
  `1`, where Exposed's default is `3`, so a transaction runs its block once. Retry a transient
  conflict with `R2dbcDatabase.retryingTransaction`, or restore Exposed's retries for every
  transaction with a `SurfDatabaseConfigurer` bean that sets `defaultMaxAttempts = 3`.
- `AtomicOperations` and `ReliabilityConfig` beans of the application replace the defaults of the
  reliability module.
- A command registered before the runtime registers its built-ins replaces a built-in. A
  `MicroserviceCommand` bean that reuses the label of a built-in fails the start.
- A bean that closes the context with `SpringApplication.exit` sets the exit code of the process.
  The `exit` command exits with `0`, a failed start with `1`.

**The NATS client name.** At 3.x a microservice created its own client, for example
`NatsClient.create("punish-service")`. In 4.0 the context names the client after the microservice,
so the default is `punish` if that is the microservice name. The client name is the queue group of
`ANY_INSTANCE` listeners and the origin of published events (`<client name>@<instance id>`).
Service queue groups come from the contract, and durable consumers do not use the client name.
The stored replies of `@NatsTransactional` services belong to the microservice name, as before.

A service whose old client name differs from its microservice name is affected while old and new
instances run together. Each queue group handles an `ANY_INSTANCE` event once, so the event is
handled twice, and a listener that filters on the origin `punish-service@…` stops matching the new
instances. To keep the old name, declare a `NatsClient` bean. A bean of the application wins over
the client of the context, and every service proxy, service and listener uses it:

```kotlin
@Bean(destroyMethod = "close")
fun natsClient(): NatsClient = NatsClient.create("punish-service")
```

Do not set `SURF_MICROSERVICE_NAME` for this: it also renames the database pool, the migration
history table `flyway_schema_history_<name>` and the owner of the stored replies. Remove the bean
once every instance of the old version is gone.

**Added.** `MigrationSpec.classNames(vararg binaryNames)` loads exactly the named migration
classes. `NatsServiceInterceptorProvider.handlesTransactional` is a member with a default, so
existing providers still compile. `SurfProperties.CONFIG_DIRECTORY` names the directory property.

Section 13 lists the plugin ids and coordinates. The version of `surf-api-database-r2dbc` defaults
to the major version of the plugin followed by `.+`, for example `4.+`. A pin such as
`surfMicroservice { database { version = "2.4.2" } }` names a coordinate that no longer exists, so
remove it or set a 4.x version.

## 15. Plugin classes and the plugin build

### The main class carries no logic

A plugin's logic lives in its beans. The main class only connects the platform's lifecycle to the
plugin's Spring context, and its lifecycle methods are `final`:

| Platform | Base class | Lifecycle |
|---|---|---|
| Paper | `SurfPaperPlugin` and `SurfPaperBootstrapper` (`dev.slne.surf.api.paper.plugin`) | `onLoad`, `onEnable`, `onDisable` and `bootstrap` are `final`. |
| Velocity | `SurfVelocityPlugin` (`dev.slne.surf.api.velocity.plugin`) | The constructor loads the plugin; surf-api enables it in its `ProxyInitializeEvent` handler and disables it in its `ProxyShutdownEvent` handler. |
| Minestom | `SurfMinestomPlugin` (`dev.slne.surf.api.minestom.plugin`) | The overridable `onLoad`, `onEnable` and `onDisable` are removed. |

Move the code of your old `onLoad`, `onEnable` and `onDisable` overrides, and of mccoroutine's
`onLoadAsync`, `onEnableAsync` and `onDisableAsync`, into `SurfLifecycle` beans:

```kotlin
@Repository
class ShopRepository {
    suspend fun warmUp() {}
}

@Service
class ShopLifecycle(private val shops: ShopRepository) : SurfLifecycle {
    override suspend fun onEnable() {
        shops.warmUp()
    }
}
```

The surf Gradle plugins generate the main class, `GeneratedSurfPlugin`, for Paper, Velocity and
Minestom, and on Paper also the bootstrapper, `GeneratedSurfBootstrapper`. Delete your own main
class unless you need one. A plugin that keeps its own class names it in the build script:
`mainClass(…)` and `bootstrapper(…)` in `surfPaperPluginApi { }`, `main` in
`velocityPluginFile { }` or `minestomPluginFile { }`. The class extends the base class of its
platform; `checkSurfPluginClasses` fails `check` and the jar tasks otherwise.
`verifyPluginClasses = false` turns the check off. A Velocity class needs an `@Inject` constructor
that takes the `PluginContainer` and passes it to `SurfVelocityPlugin`.

`generatePluginClass = false` turns generation off for a module that applies a platform plugin but is
not a plugin itself.

### Component scanning and the base package

Spring scans the plugin's own jar, starting at the plugin's **base package**. The Gradle plugin
computes it as the longest common package of the main sources and puts the generated classes
there. Set `basePackage` in the platform block when the sources share no package, or only one that
contains surf-api's own, such as `dev.slne` or `dev.slne.surf`; the build rejects those. With a named
class, the scan starts at that class's package (on Paper, the bootstrapper's), and `basePackage` has
no effect. The build warns when that package misses part of the sources.

### Plugin dependencies and exports on Paper

A plugin that injects another surf plugin's beans declares it with `surfPluginDependency("NAME")`, or
`softSurfPluginDependency("NAME")` when the plugin also runs without it, in `surfPaperPluginApi { }`.
Replace `NAME` with the other plugin's name. Both register the plugin as bootstrap and server
dependency, loaded before, with joined class path. Declare plugins that are not surf plugins with
`bootstrapDependencies { }` and `serverDependencies { }` as before. The Gradle plugin adds surf-api
and CommandAPI as required server dependencies itself.

A plugin exports a bean by implementing an interface annotated with `@PluginApi`. Only plugins that
declare the provider as a direct dependency see the export; every other bean stays private to the
plugin. For the export rules, see the Exports section of
[docs/spring-migration.md](docs/spring-migration.md).

On Paper, each plugin also has a **bootstrap context**, built in Paper's bootstrap phase. It holds
only the plugin's beans that implement `SurfBootstrapLifecycle` or carry `@BootstrapComponent`. A
bootstrap bean must not use the Bukkit server API or the API of a plugin that is not a bootstrap
dependency. Of surf-api's own beans, it sees only surf-api's bootstrap components.

### The plugin instance is a bean

The plugin's main object is the primary bean of its plugin's context on Paper, Velocity and
Minestom. Spring injects its `@Autowired` fields and setters, runs the `Aware` callbacks,
`@PostConstruct` and every `BeanPostProcessor` on it, and runs `@PreDestroy` and
`DisposableBean.destroy()` when the context closes. Spring does not call an inferred `close()` or
`shutdown()` method. A plugin that cleans up in a disable hook and again in `@PreDestroy` or
`destroy()` cleans up twice.

Inject the instance by its concrete type, or as `org.bukkit.plugin.Plugin` on Paper.

### Build checks and compiler settings

- **Spring ships inside surf-api.** A Paper, Velocity or Minestom plugin compiles against it with
  `compileOnly` and must not bundle it. `checkNoBundledSpring` runs with `check` and before the
  shadow jar tasks. It fails when the shadow jar would bundle unrelocated `org/springframework/`
  classes, whether they come from a Spring coordinate or inside another artifact, such as a fat jar.
  The failure names the artifact. `allowBundledSpring = true` in the platform block turns the check
  off.
- **The surf Gradle plugins apply `kotlin("plugin.spring")` and set `javaParameters = true`.**
  Classes annotated with Spring stereotypes are open, and constructor parameter names reach the
  bytecode.
- **The KSP processor `surf-api-processor` generates only the NMS provider index and the
  `@GenerateReflection` accessors.** Its unused annotation helpers are removed:
  `findAllByDistance`, `findAnnotationRecursive`, `findAllAnnotationsRecursive`,
  `findMetaAnnotations`, the `KSAnnotated.findAnnotationRecursive` and
  `KSAnnotated.findAllAnnotationsRecursive` extensions, both `collectArgumentValues` overloads,
  `collectArgumentValuesGrouped` and `AnnotationUtils.FoundAnnotation`.
  `AnnotationUtils.findAnnotatedClasses(resolver, annotationFqName)` finds direct annotations only.
- **`NmsProvider` implementations are found through `META-INF/surf-api/nms-providers/*.json`**,
  which the KSP processor generates. The NMS modules ship no
  `META-INF/services/dev.slne.surf.api.paper.nms.common.NmsProvider` file.

### Standalone programs

`SurfApiStandaloneBootstrap.bootstrap()` takes an optional `application` descriptor; with one, the
root context also hosts the application's beans. `SurfApiStandaloneBootstrap.adopt(context,
application)` makes a Spring Boot program's own context surf-api's root context.

`SurfApiStandaloneBootstrap.enable()` registers the root context's beans with `@SurfEventHandler`
methods on the event bus, and `shutdown()` unregisters them; a microservice gets the same through
`adopt`. Remove your own `registerListeners` and `unregisterListeners` calls for these beans;
otherwise every handler fires twice.

## 16. mccoroutine was removed

surf-api no longer ships or exposes mccoroutine on Paper or Velocity. `SuspendingJavaPlugin`, the
plugin scope, `launch` and the entity, region and global-region dispatchers have replacements in
surf-api:

| mccoroutine | 4.0 |
|---|---|
| `SuspendingJavaPlugin`, `onEnableAsync` and the other suspending phases | `SurfPaperPlugin` and `SurfLifecycle` beans (section 15) |
| `plugin.scope`, `plugin.launch { }` | Inject `CoroutineScope` and call `scope.launch { }`. |
| `plugin.entityDispatcher(…)`, `plugin.regionDispatcher(…)`, `plugin.globalRegionDispatcher` and mccoroutine's other dispatchers | Inject `PaperDispatchers` (`dev.slne.surf.api.paper.coroutine`) and use `entity(entity)`, `region(…)`, `main` or `async`. |
| `forEachPlayerInRegion(SuspendingPlugin, …)` | `forEachPlayerInRegion(Plugin, …)` |

Each plugin context holds its own `CoroutineScope` bean. It has a `SupervisorJob`, is cancelled
when the plugin is disabled, and dispatches on `Dispatchers.Default` by default, where mccoroutine's
`plugin.scope` dispatched on the server thread. Code that has to run on the server thread asks for
it explicitly:

```kotlin
@Service
class Rewards(
    private val scope: CoroutineScope,
    private val dispatchers: PaperDispatchers,
) {
    fun reward(player: Player) {
        scope.launch {
            val amount = loadAmount(player.uniqueId)
            player.withEntity { it.giveExp(amount) }
            withContext(dispatchers.main) { player.updateInventory() }
        }
    }

    private suspend fun loadAmount(id: UUID): Int = 10
}
```

`entity.withEntity { }` runs a non-suspending block on the thread that owns the entity. If the
entity is removed or the plugin disabled before the block runs, the block never runs and the call
throws a `CancellationException`. `Int.ticks` converts ticks to a `Duration`, 50 ms per tick.

On Paper, the default scope of the suspending command executors, requirements and arguments is the
declaring plugin's scope with its main dispatcher, so they keep starting on the server thread. The
public `extractCallingPluginScopeOrThrow` is removed; the executors find the scope themselves. On
Velocity and Minestom, the suspending executors, and Velocity's `SuspendCustomArgument`, default to
a shared surf-api scope that belongs to no plugin. To run them in the plugin's scope, pass the
injected `CoroutineScope` as their `scope` argument.

Uncaught failures in a plugin's scope are logged by the commons-logging logger `surf-coroutines`
with the message `Unhandled exception in a coroutine of '<plugin>'`, and the scope's
`CoroutineName` is `surf-<plugin>`. On Minestom, `<plugin>` is the plugin id. Minestom's 3.x scope
was named `plugin-<id>` and logged through the plugin's `ComponentLogger`. Log filters and appenders
keyed on that logger name have to match `surf-coroutines` or the plugin id in the message. A
Minestom plugin without a jar of its own runs its scope's coroutines with the server class loader
as thread context class loader.

## 17. Configuration, listeners and commands as beans

### Configuration

Declare a plugin's config file with `@SurfConfig` and inject a `ConfigHandle`:

```kotlin
@ConfigSerializable
@SurfConfig("shop.yml")
data class ShopConfig(val discount: Double = 0.0)

@Service
class Pricing(private val config: ConfigHandle<ShopConfig>) {
    fun price(base: Double) = base * (1 - config.value.discount)
}
```

The path is relative to the plugin's data folder, and its extension picks the format: `.yml` or
`.yaml` for YAML, `.json` for JSON. The file is read, migrated and written back with its defaults
when the plugin's context starts, so a broken file fails the plugin's start. `ConfigHandle` offers
`value`, `reload()`, `edit { }`, `update { }` and `save()`. `@SurfConfig` also works as a
meta-annotation on an annotation of your own.

`SpongeYmlConfigClass` and `SpongeJsonConfigClass` companions keep working. A plugin registers
serializers for its own types through a `ConfigSerializerRegistrar` bean, which runs before the
plugin's first handle opens its file; `surfSpongeConfigSerializers` is removed.

### Listeners and commands

In the enable phase, surf-api registers the listener and command beans of a plugin context, before
the `onEnable` hooks run, and unregisters the listeners when the plugin is disabled:

- Paper: beans implementing `Listener`
- Velocity: beans with `@Subscribe` methods
- Minestom: beans of type `EventNode`, attached to the plugin's event node
- every platform: beans with `@SurfEventHandler` methods
- commands: beans extending `CommandAPICommand` or `CommandTree`, and `CommandRegistrar` beans that
  call the command DSL in `register()`

```kotlin
@Service
class ShopService {
    suspend fun open(player: Player) {}
}

@Component
class ShopCommands(private val shop: ShopService) : CommandRegistrar {
    override fun register() {
        commandAPICommand("shop") {
            playerExecutorSuspend { player, _ -> shop.open(player) }
        }
    }
}
```

Remove your own registration calls for these beans; otherwise surf-api registers them a second
time.

On Velocity the listener beans are registered while Velocity fires `ProxyInitializeEvent`, so their
handlers for that event never run. A bean that has to act once the proxy is up overrides
`SurfLifecycle.onEnable`.

## 18. The legacy scoreboard API was removed

The package `dev.slne.surf.api.paper.scoreboard` is gone: `SurfScoreboardApi`, `SurfScoreboard`,
`SurfScoreboardBuilder`, `SurfAutoUpdatableScoreboard`, `SurfAutoUpdatablePlayerScoreboard` and the
helpers of `scoreboard-extensions.kt`. It had been deprecated since 3.46.0. Use the sidebar API in
`dev.slne.surf.api.paper.sidebar`.

surf-api no longer depends on `net.megavex:scoreboard-library`. `surf-api-paper` exported
`scoreboard-library-api`, so a plugin that used `net.megavex` classes through surf-api has to
declare the library itself.

Pass your own plugin to the sidebar functions. A sidebar is active once created and closes when
that plugin is disabled.

| 3.x | 4.0 |
|---|---|
| `SurfScoreboardBuilder.builder(title)` … `build()` / `enable()` | `sidebar(plugin) { title(title) … }`, returns `SurfViewerSidebar` |
| `buildAutoUpdatable()` | `autoUpdatingSidebar(plugin) { updateInterval = … }`, returns `SurfAutoUpdatingSidebar`. The interval is configurable; the old one was 5 ticks. |
| `buildAutoUpdatablePlayer()` | `globalSidebar(plugin) { … }`, returns `SurfGlobalSidebar` |
| `addLine(text)` | `line(text)` |
| `addUpdatableLine(supplier)` | `sharedLine { … }` |
| `addViewerLine(function)` | `line { viewer -> … }` |
| `addViewerComponent(function)` | `buildViewerLines { viewer -> … }` |
| `addAnimatedLine(frames)` | `animatedLine(frames)` |
| `addGradientLine(text, start, end)` | `gradientLine(text, start, end)` |
| `addLineSeparator()` | `separator()` |
| `disable()` | `close()`; a closed sidebar cannot be enabled again |
| `buildSidebarComponent { }` | `buildViewerLines { viewer -> buildLine { … } }` |
| `buildStaticLine { }`, `buildDynamicLine { }` | `buildLine { … }` |
| `buildStaticLine(ScoreFormat.fixed(x)) { }` | `buildLine(score = x) { … }` |

What the sidebar API does not cover:

- There is no `scoreboardLibrary()` replacement for custom teams and objectives, and no custom
  `SidebarAnimation`.
- A score is a fixed `Component`; other `ScoreFormat` values are not supported.
- Viewer lines render on a background dispatcher, not on the viewer's entity thread. A line that
  reads entity or world state switches with `withContext(dispatchers.entity(viewer))`.
- The sidebar API is Kotlin-only, so a Java plugin cannot build sidebars with dynamic lines.

## 19. Dependencies and published jars

**DataFixerUpper.** surf-api no longer depends on `com.mojang:datafixerupper`: `surf-api-core-api`,
`surf-api-nbt-api` and `surf-api-serialization-api` do not export it, and the surf Gradle plugins do
not relocate `com.mojang.serialization` or `com.mojang.datafixers`. On Paper, a plugin's DFU
references bind to the server's own `com.mojang.serialization`. A plugin that used DFU through
surf-api declares it itself. The DFU-based API is removed without replacement:

- `dev.slne.surf.api.nbt.serializer.NbtOps`
- the `Codec` extensions in `dev.slne.surf.api.serialization` (`codec-extensions.kt`):
  `Codec.xor`, `fixedSize`, `Codec.ranged`, `Codec.positive`, `positiveInt`, `positiveLong`,
  `positiveDouble`, `positiveFloat`, `positiveShort`, `positiveByte`, the `component1` and
  `component2` operators of DFU's `Pair`, and `toDataFixerPair`
- the codec holders `LocalTimeCodec`, `ZoneIdCodec` and `ZoneOffsetCodec`. The kotlinx
  serializers `LocalTimeSerializer`, `ZonedIdSerializer` and `ZoneOffsetSerializer` stay.

**Reactor Netty and Netty.** `surf-api-core-api` does not export `reactor-netty-core`,
`reactor-netty-http` or `io.netty`. A plugin that used them through surf-api declares them; on Paper
and Velocity, compile against the server's Netty with `compileOnly`. On Minestom and standalone,
`surf-api-minestom-impl` and `surf-api-standalone-impl` ship only `netty-buffer` and `netty-common` at
runtime; code that needs another Netty module, such as transport, handler or codec, brings it
itself.

**Ktor and OkHttp.** `surf-api-core-api` does not export the Ktor client (`ktor-client-core`,
`ktor-client-okhttp`, content negotiation, `ktor-serialization-kotlinx-json`) or OkHttp, and the
platform modules no longer ship OkHttp at runtime. A plugin that used either through surf-api
declares and bundles it itself, or uses the JDK's `java.net.http.HttpClient`.

**Guava, Gson and SLF4J on Paper.** The surf-api Paper plugin does not bundle Guava, Gson or
`slf4j-api`. Plugins that reach them through surf-api's joined class path get the Paper server's
versions.

**BouncyCastle relocation.** The surf-api Paper and Velocity plugins relocate
BouncyCastle, which jnats uses for NKey authentication, together with `io.nats` to
`dev.slne.surf.nats.libs`, and leave out BouncyCastle's native `libbc` libraries. BouncyCastle then
runs its Java implementation. The shaded jars of `infra/nats`, such as the `-all` jar of
`surf-api-nats-standalone`, relocate BouncyCastle the same way; they relocated only `io.nats`
before. Code that used `org.bouncycastle` classes through such a jar declares BouncyCastle itself.
On Minestom, `io.nats` and BouncyCastle stay unrelocated.

**No `-all` variants.** The surf-api modules built on surf-api's own conventions (every api, impl and
aggregate module outside `infra/`) do not publish a `shadowRuntimeElements` variant, so no
coordinate such as `dev.slne.surf.api:surf-api-core-api:4.0.0:all` exists. Depend on the plain
artifact instead of the `all` classifier or `Bundling.SHADOWED`. The shaded runtime jars of
`surf-api-paper-impl`, `surf-api-velocity-impl` and `surf-api-standalone-impl` are attached to the
GitHub release; their plain artifacts stay in the Maven repository. The `shadow` publications of
the `infra/` libraries, such as the `-all` jars of `surf-api-redis-api` and
`surf-api-redis-spring`, are unchanged.

**Relocation helpers of the Gradle plugin.** `CommonSurfPlugin.addRelocationsForDependency` is
removed without replacement; add relocations on the `shadowJar` task. The plugins' shadow jar tasks
no longer apply a `dev.slne.surf.redis` relocation for a project dependency.

## 20. Other removed API

| Removed | Replacement |
|---|---|
| `requiredService<T>()` | Constructor injection (section 8) |
| `SurfMinestomPlugin.onLoad`, `onEnable`, `onDisable` | `SurfLifecycle` beans (section 15) |
| `extractCallingPluginScopeOrThrow` | Nothing; see section 16. |
| The `object` `PlayerLookupService` and its nested `LookupResult` | `PlayerLookupService` is an interface now; inject it. `getUuid`, `getUsername` and `getCacheStats` keep their signatures. |
| `dev.slne.surf.api.shared.api.annotation.AnnotationUtils` (`findAnnotation`, `clearCache`) | Spring's `org.springframework.core.annotation.AnnotationUtils.findAnnotation(type, annotationType)`, which searches the same places and caches; `MergedAnnotations` when attributes or `@AliasFor` matter. |

## 21. Checklist for a downstream repository

1. Replace any coordinate from section 3; the others keep working.
2. Rewrite the moved package prefixes (section 6), `core.util` → `common.util` and the rest. This
   touches the most files, and `logger()` accounts for most of them. No build file changes.
3. Remove every fastutil type and every `to*` bridge helper (section 7). Compile.
4. Check your `@ConfigSerializable` classes for fastutil-typed fields (section 7).
5. If you resolve an implementation class at runtime through an `-api` aggregate, add the matching
   `-impl` coordinate (section 2).
6. Rewrite every inventory import that names one of the types in section 12; the other
   inventory imports are unchanged.
7. Replace every companion call, `INSTANCE` and `instance` read of a surf-api type with an injected
   constructor parameter (section 8). The compiler lists them all.
8. If you published an implementation of a surf-api interface with `@AutoService`, make it a bean
   (section 8).
9. Turn every `SurfComponent` into a Spring bean, its phases into `SurfLifecycle` hooks and its
   conditions into Spring conditions (section 9). This is the largest item on the list.
10. Delete your plugin's main class or reduce it to an empty subclass of the platform base class,
    and move its lifecycle code into beans (section 15). Declare surf plugin dependencies with
    `surfPluginDependency` and export shared beans through `@PluginApi`.
11. Replace mccoroutine: inject `CoroutineScope` and `PaperDispatchers`, and dispatch to the server
    thread explicitly where code needs it (section 16).
12. Move config files to `@SurfConfig` with `ConfigHandle` where a bean reads them, and serializer
    registration to `ConfigSerializerRegistrar` beans (section 17).
13. Remove your own listener and command registration for beans that surf-api registers
    (section 17).
14. Search for `dev.slne.surf.api.paper.scoreboard` and `net.megavex`, and move scoreboards to the
    sidebar API (section 18).
15. Remove any bundled Spring from your plugin jar, and declare DataFixerUpper, Reactor Netty,
    Netty, Ktor or OkHttp yourself where you used them through surf-api (sections 15 and 19). Replace dependencies
    on `-all` coordinates with the plain artifact.
16. Replace every surf-nats, surf-redis, surf-database-r2dbc and surf-microservice coordinate and
    plugin id, delete every `withSurfDatabaseR2dbc(…)` call, replace `withSurfRedis(version,
    relocation)` with `withSurfRedis()`, and remove the surf-redis and surf-nats plugin
    dependencies and jars from the servers; surf-api copies their config on first start
    (section 13).
17. Turn every microservice into a Spring Boot application, and check its NATS client name if a
    rolling deploy mixes old and new instances (section 14). If a service relied on Exposed's
    implicit retries, call `retryingTransaction` or restore them in a `SurfDatabaseConfigurer`.
18. Add `surf-api-standalone-testing` to the test runtime of modules whose tests use surf-api's
    beans (section 13).
