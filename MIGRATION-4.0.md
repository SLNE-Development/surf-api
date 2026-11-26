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

## 4. The Minestom feature artifacts were renamed, without aliases

The eight Minestom server features moved from `surf-api-minestom-server-<feature>` to
`surf-api-minestom-<feature>-impl`. 4.0 publishes no relocation POM for the old names, so a build
file that names one fails to resolve it:

`bootstrap`, `console`, `luckperms`, `npc`, `player-visibility`, `plugins`, `signed-chat`,
`spark`.

You do not normally name these at all: the `withXxx()` calls in the Gradle block
`surfMinestomServerApi { }` put the features you ask for on the class path.

## 5. Added artifacts

| Artifact | What it is |
|---|---|
| `surf-api-registry-keys` | The four generated key holders (`BlockTypeKeys`, `ItemTypeKeys`, `SoundKeys`, `VanillaAdvancementKeys`) that used to sit inside `surf-api-core` and only change with the Minecraft version. A separate module, so a core change does not recompile them. All four are Kotlin `object`s with `@JvmField` keys now: `BlockTypeKeys.STONE` reads as before, and the fields keep their `public static final Key` descriptors. |
| `surf-api-spring-api` | The contracts a plugin writes its beans against: `SurfLifecycle`, `SurfBootstrapLifecycle`, `@PluginApi`, `@BootstrapComponent` and `@ConditionalOnPlugin`. Exports `spring-context`. |
| `surf-api-spring-impl` | The context manager behind the root, import and plugin contexts. |
| `surf-api-standalone-testing` | Starts surf-api's standalone runtime once per test JVM; see section 13. |
| `surf-api-bom` | A platform BOM listing every published artifact at one version. Prefer importing it over pinning versions one by one. |
| `surf-api-inventory-api` | The platform-neutral half of the inventory framework: view settings, header geometry, text alignment, the container component base and the DSL markers. Paper and Minestom each carried their own copy; see section 12. |
| `surf-api-common-api` | The shared base `surf-api-core` grew around: `util` (including `logger`), `collection`, `math`, `random`, `font`. Nothing in it names anything else of surf-api's, so every other domain can depend on it. |
| `surf-api-serialization-api` | The kotlinx-serialization serializers: `SurfSerializerModule`, the Adventure serializers, the JDK ones (UUID, dates, IP, numbers) and the SpongePowered math ones. |
| `surf-api-config-api` | The configuration surface: `@SurfConfig`, `ConfigHandle`, `ConfigHandles`, `configHandle` / `surfConfig`, the 23 constraint annotations, the helper types and the migrations. |
| `surf-api-config-impl` | The `ConfigHandle` registration for plugin contexts. |
| `surf-api-messages-api` | The text surface: `Colors`, `CommonComponents`, the `buildText` / `sendText` builders, the colour palettes, the message bundles, pagination, the configured MiniMessage instance, the action bar and `Rarity`. |
| `surf-api-event-api` | `SurfEventBus` and the event contracts: `SurfEvent`, `SurfSyncEvent`, `SurfAsyncEvent`, `SurfCancellableEvent`, `@SurfEventHandler`, `SurfEventPriority`. |
| `surf-api-nbt-api` | The Adventure binary-tag surface: `CollectionBinaryTag` and `FastCompoundBinaryTag`. |
| `surf-api-reflection-api` | `@ReflectionProxy` interfaces and `reflectionProxy<T>()` (section 20). Separate from nbt: they share no import, and a consumer of the NMS layer should not need a tag module to get a reflection proxy. |
| `surf-api-command-api` | What all three platforms' command layers share: `SurfCommandUtil`, `CommandExceptionBuilder`, `WrapperCommandExceptionComponent`, `SuspendArgumentHelper` and `CommandRegistrar`. A module of its own, so that a plugin pulling in the colour palette does not also get CommandAPI on its compile classpath. |
| `surf-api-luckperms-api` | `SurfLuckPerms` and the LuckPerms extensions. A module of its own for the same reason. |

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

surf-api no longer exposes or exports [fastutil]. Its public signatures use the JDK
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

surf-api's config serializers covered **39 fastutil map types**. Those serializers are gone. A
`@ConfigSerializable` class with a field typed as a fastutil map is no longer serialisable and
fails on load. Change such a field to the JDK map, `Map<K, V>`, which Configurate handles.

### Where fastutil stays

These modules keep it on the **compile** classpath only, so it never reaches yours:

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
or carry an `instance` property annotated `@InternalSurfApi`. That annotation is an
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
| `SurfConfigApi.instance`, `surfConfigApi` | Removed with `SurfConfigApi`. Declare the file with `@SurfConfig` or open it with `ConfigHandles.open` (section 17). |
| `surfSpongeConfigSerializers` | Removed. Declare each serializer as a `@SurfConfigSerializer` class (section 17). |
| `SurfEventBus.INSTANCE`, `SurfEventBus.registerListeners(…)` and the other delegated calls | Inject `SurfEventBus`, or annotate methods of a bean with `@SurfEventHandler` (section 17). `call()` on an event is unchanged. |
| `SurfReflection.INSTANCE`, `surfReflection` | Removed with `SurfReflection`. Inject the `@ReflectionProxy` interface or call `reflectionProxy<T>()` (section 20). |
| `ActionBarService.instance` | Removed. Call the `Audience.sendActionBar(…)` extension, or inject `ActionBarService`. |
| `InternalPaginationBridge.INSTANCE` | Internal to surf-api; no replacement. |
| `InternalNbtBridge.INSTANCE` | Removed. `CompoundBinaryTag.fast()` no longer needs a bean. |
| Paper: `SurfGlowingApi`, `SurfPaperPacketApi`, `SurfPaperPacketListenerApi`, `SurfPaperVisualizerApi`, `TickThreadGuard`, `SurfPaperPAPIHook`, `SuspendRequirementService` | Inject the interface. |
| Paper: `BlockPdcProvider.INSTANCE` | Call the `Block.pdc()` extension, or inject `BlockPdcProvider`. |
| Paper NMS: `SurfPaperNmsBridge`, `SurfPaperNmsCommonBridge`, `SurfPaperNmsCommandArgumentTypesBridge`, `SurfPaperNmsEntityBridge`, `SurfPaperNmsGlowingBridge`, `SurfPaperNmsItemBridge`, `SurfPaperNmsLootTableBridge`, `SurfPaperNmsNbtBridge`, `SurfPaperNmsPlayerBridge`, `SurfPaperNmsPortalBridge`, `SurfPaperNmsStatsBridge`, `SurfPaperNmsPacketBridges`, `SurfPaperNmsBlockPackets`, `SurfPaperNmsSpawnPackets`, `SurfPaperNmsPlayerChatPackets`, `SurfPaperNmsPlayerPackets`, `SurfPaperNmsPlayerToastPackets` | Inject the interface. |
| Paper: `SurfSidebarFactory.INSTANCE` | Call `sidebar(plugin) { }`, `autoUpdatingSidebar(plugin) { }` or `globalSidebar(plugin) { }`. |
| Paper: `SurfScoreboardApi.INSTANCE` | Removed with the scoreboard API; see section 18. |
| Paper and Minestom: `ViewFrameAccessor.INSTANCE`, the top-level `viewFrame` | Inject `ViewFrame` (`me.devnatan.inventoryframework.ViewFrame`) or `ViewFrameAccessor`. The extension functions of the inventory framework, such as `register()`, keep working. |
| `SurfComponentApi`, `surfComponentApi` | Removed; see section 9. |
| `LuckPermsAccess.luckperms`, `getUser`, `loadUser`, `getOrLoadUser` | Inject `SurfLuckPerms` (`luckPerms`, `getUser`, `loadUser`, `getOrLoadUser`, `prefix`, `suffix`, `metaValue`), or inject `LuckPerms` itself as `ObjectProvider<LuckPerms>` or a `@Lazy` parameter: LuckPerms enables after the root context is built. Outside a bean, call `uuid.getLuckPermsUserOrNull()`, `uuid.getLuckPermsUser()` or `uuid.loadLuckPermsUser()`. On Minestom `SurfLuckPerms` is a root bean when the server build includes the LuckPerms feature module (Gradle `withLuckPerms()`), and its members throw until the runtime feature has loaded; the `LuckPerms` bean exists only between feature load and disable. Without the module, inject `ObjectProvider<SurfLuckPerms>`. |

`PluginManager.instance` on Minestom stays public.

### What else changed

- **`@AutoService` does not publish an implementation to surf-api.** surf-api asks Spring, not
  `ServiceLoader`. If you registered an implementation of a surf-api interface through
  `@AutoService` and expected surf-api to find it, make it a bean instead.
- **A service is available once surf-api's root context runs.** surf-api builds the root context
  before any plugin loads. Code that runs during surf-api's own class initialisation, before the
  root context exists, gets an `IllegalStateException` saying that surf-api's root context has not
  been started yet.
- **Bean metadata is not generated at build time.** No `META-INF/surf-api/beans/` index is
  written. Spring finds a plugin's beans by scanning its jar;
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
  main thread and block it. A hook may wait for work on the injected `PaperDispatchers.main`,
  which the blocked phase runs itself; it must not wait for a task handed to the Bukkit scheduler
  directly, or on Folia for region or entity work. Constructors and `@PostConstruct` only wire
  objects.

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

## 11. Compatibility shims were removed

3.x kept old signatures, hidden overloads and deprecated declarations so that plugins compiled
against an earlier release still linked. A plugin compiled against 3.x does not link against 4.0.0
anyway, so 4.0.0 drops all of them:

- **Hidden declarations.** The inventory framework's `@Deprecated(level = DeprecationLevel.HIDDEN)`
  overloads and `ViewContainerBackHintComponent`, the hidden `CommonComponents.getEM_DASH()` next to
  the `EM_DASH` field, and the hidden non-inline `PacketOperation(block)` in `PacketOperation.kt`. The inline one was named `PacketOperationInline` on the JVM and is simply
  `PacketOperation` now. A hidden declaration was never visible from source, so you will not notice.
- **`@IntroducedAt` parameters.** Parameters added after 3.0 no longer have a synthetic overload
  with the old signature. Source that passes or omits them compiles as before; only bytecode
  compiled against the old overload has to be recompiled. This covers
  `CoroutineScope.runAtFixedRate` and `runWithFixedDelay` (`catchExceptions`, `taskName`),
  `miniMessageArgument` and the `MiniMessageArgument` constructor (`greedy`),
  `SurfApiStandaloneBootstrap.bootstrap` (`application`) and `enable` (`registerShutdownHook`),
  and the Paper and Minestom `SimpleViewSettings` and `PaginatedViewSettings` constructors and
  their `copy`.
- **`@JvmOverloads` and `@JvmStatic`.** No declaration generates Java overloads or static
  bridges any more; section 20 lists them under "Java consumers are no longer supported".
- **Deprecated declarations.**

| Removed | Replacement |
|---|---|
| `RandomSelector.pick(RandomGenerator)`, `flow(RandomGenerator)` | `pick()`, `flow()`; pass the generator when you create the selector |
| `User.getMeta<T>(key)`, `User.getMeta<T>(key, default)` | `getMetaValue(key)` for the raw string; for other types a parsing transform such as `getMeta(key) { it.toIntOrNull() }` or `getMeta(key, default) { it.toIntOrNull() }` |
| `RedisApi.create(pluginDataPath, pluginsPath, serializerModule)` | `RedisApi.create(serializerModule)` |
| `SurfPaperNmsNbtBridge.getNbtString(itemStack, key)` | No direct replacement; Paper exposes no custom data component. Read item data through `itemStack.persistentDataContainer` and write it with `itemStack.editPersistentDataContainer { }`. PDC values live under `PublicBukkitValues`, so raw keys at the custom data root that other code wrote are not readable this way |
| `forEachPlayerInRegion(action, concurrent)` | `forEachPlayerInRegion(plugin, action, concurrent)` |
| `SurfVisualizer.viewers` (`Set<Player>`) | `viewerUuids`; resolve online players with `Bukkit.getPlayer(uuid)` |
| `BlockStateFactory`, its `Builder` and `BlockStateFactoryImpl` in `dev.slne.surf.api.common.util.blockstate` | packetevents' `WrappedBlockState` directly: `stateType.createBlockState()` and its setters (`setAge`, `setFacing`, ...) |

- **Relocation POMs.** `publishRelocationFrom` is gone from the build; see section 4.

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

**The standalone modules are removed.** `surf-api-nats-standalone` (`SurfNatsStandalone`, `NatsInstance`) and
`surf-api-redis-standalone` (`StandaloneRedisInstance`) are gone. NATS runs in a Spring context: the plugin context
of a surf-api plugin, or a Spring Boot context that `SurfApiStandaloneBootstrap.adopt` makes surf-api's root context
with `surf-api-nats-spring` on the class path, as in a microservice. Redis runs in the plugin contexts of Paper,
Velocity and Minestom. A program that started surf-nats itself becomes such a Spring Boot application, usually a
microservice. `RedisInstance` is internal API (`@InternalSurfApi`): only the surf-api runtimes implement it, so a
program can no longer provide its own. Outside Paper, Velocity and Minestom, the runtime for Redis is surf-api
standalone, which does not bundle the Redis integration yet.

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

**The static factories need surf-api's root context.** `RedisApi.create(...)` and
`NatsClient.create(...)` no longer find their implementations through `ServiceLoader`; they read
beans of surf-api's root context. A call before that context exists, such as in a Paper plugin's
bootstrapper, fails with `IllegalStateException`. surf-redis and surf-nats ship no
`META-INF/services` files for them any more, so a shaded copy needs no service-file merging for
these lookups. Inject `RedisApi` and `NatsClient` instead of creating them where you can.

**Tests that use surf-api beans.** surf-api's beans, and the API that reads them such as
`ConfigHandles.open`, `RedisApi.create` and `NatsClient.create`, work only while surf-api's root context runs, which a plain JUnit test never starts. Add
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
| `MigrationSpec.classPackage(...)` | `classes(...)` or `classNames(...)`. A microservice needs neither: it runs every `SurfMigration` below the package of its `@SpringBootApplication` class. |

**Behavior that changed.**

- The context creates the NATS client and connects it. Delete your own `NatsClient.create(...)`.
- The database opens and migrates while the context starts, before the surf-api hooks, so a
  schema fix in `onEnable` comes too late for beans that already injected `R2dbcDatabase`.
- A `@NatsService`, `@NatsTransactional` or `@NatsListener` bean is always created eagerly, even
  with `spring.main.lazy-initialization=true`. A bean of these kinds that is first created after
  the application was enabled fails.
- A `@NatsTransactional` bean needs the reliability module of surf-microservice; without it the
  start fails. `NatsServiceInterceptorProvider` and `Class.isNatsTransactional()` are internal
  (`@InternalNatsApi`): custom interceptor providers are not supported.
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
classes. `SurfProperties.CONFIG_DIRECTORY` names the directory property.

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
| Velocity | `SurfVelocityPlugin` (`dev.slne.surf.api.velocity.plugin`) | Guice's member injection loads the plugin after its constructor; surf-api enables it in its `ProxyInitializeEvent` handler and disables it in its `ProxyShutdownEvent` handler. |
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
`verifyPluginClasses = false` turns the check off. `SurfVelocityPlugin` has a no-argument
constructor, so a Velocity class takes only what it needs itself:

```kotlin
// before
class ShopPlugin @Inject constructor(container: PluginContainer) : SurfVelocityPlugin(container)
// after
class ShopPlugin @Inject constructor(val proxy: ProxyServer) : SurfVelocityPlugin()
```

The plugin's context is built once the constructor and the property initializers finished, so the
plugin instance is complete when beans receive it. `@Inject` fields and methods of the subclass are
injected after the context loaded.

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

### Platform beans in every context

surf-api registers three beans that 3.x did not have:

- `surfPluginLogger`, the plugin's `ComponentLogger`, in every full plugin context on Paper,
  Velocity and Minestom. It also satisfies an injection of `org.slf4j.Logger`. It is a fallback
  bean: a `Logger` or `ComponentLogger` bean that the plugin declares itself wins, so an existing
  logger bean keeps working without `@Primary`.
- `surfBukkitServer`, the `org.bukkit.Server`, in surf-api's root context on Paper. Every plugin
  context can inject it. A plugin that declares its own `Server` bean now has two candidates and
  marks its own `@Primary` or injects it with `@Qualifier`.
- `surfApiPlugin`, surf-api's own `Plugin`, in surf-api's root context on Paper. It is a fallback
  bean, so `Plugin` in a plugin context still resolves to the plugin itself; only a
  `List<Plugin>` or `ObjectProvider<Plugin>` injection also sees it.

### Build checks and compiler settings

- **Spring ships inside surf-api.** A Paper, Velocity or Minestom plugin compiles against it with
  `compileOnly` and must not bundle it. `checkNoBundledSpring` runs with `check` and before the
  shadow jar tasks. It fails when the shadow jar would bundle unrelocated `org/springframework/`
  classes, whether they come from a Spring coordinate or inside another artifact, such as a fat jar.
  The failure names the artifact. `allowBundledSpring = true` in the platform block turns the check
  off.
- **`checkInternalApiUsage` fails the build when plugin code uses internal API.** It runs with
  `check` and reads the bytecode of the main classes. A reference fails when the referenced
  declaration of a dependency, or a class enclosing it, carries an annotation that is itself
  annotated `@InternalAPIMarker`: `@InternalSurfApi`, `@InternalRedisAPI`,
  `@InternalMicroserviceApi` or another plugin's own marker. An inherited member counts where it is
  declared, so a call to an internal member of a public base class fails too, and so does a call
  that relies on an internal function's or constructor's default arguments.
  `@PublishedSurfApi` declarations and references between the project's own classes pass;
  `@OptIn` does not exempt a reference. Replace the internal calls with public API. A surf library
  built against that internal API allows the marker in the platform block:
  `allowInternalApi("dev.slne.surf.api.shared.api.util.InternalSurfApi")`. The check does not cover
  `@InternalNatsApi`, a multiplatform marker that carries no `@InternalAPIMarker`; its `@OptIn`
  requirement still applies.
- **`withApiValidation()` no longer excludes `@InternalSurfApi` from the ABI dump.** Pass the
  markers to exclude: `withApiValidation("com.example.InternalExampleApi")`. Without arguments the
  dump keeps every public declaration.
- **The plugin file's website defaults to the `origin` Git remote.** `paper-plugin.yml`'s `website`
  and the `url` of `velocity-plugin.json` and `minestom-plugin.json` take, in this order, the value
  set in the build script, the `url` Gradle property and the `origin` remote as an `https` URL:
  `git@github.com:org/repo.git` becomes `https://github.com/org/repo`, and credentials, port and
  `.git` are dropped. Set the property or the value explicitly to publish another URL. Paper reads
  `url` through `providers.gradleProperty` like Velocity and Minestom, so `ext.url` in a build
  script and `url` in a subproject's own `gradle.properties` no longer count; use the root
  `gradle.properties`, `-Purl=...` or the `paper { website = ... }` block.
- **The surf Gradle plugins apply `kotlin("plugin.spring")` and set `javaParameters = true`.**
  Classes annotated with Spring stereotypes are open, and constructor parameter names reach the
  bytecode.
- **`surf-api-processor` is removed.** The surf Gradle plugins no longer add it to the `ksp`
  configuration; they still apply the KSP Gradle plugin. Its only remaining job, the NMS provider
  index under `META-INF/surf-api/nms-providers`, is gone with the provider (section 20).

### Spring Boot conditions

Every runtime now ships `spring-boot` and `spring-boot-autoconfigure`, and the surf-api api
modules export them for compilation like Spring itself. Plugins can use Boot's conditions, such as
`@ConditionalOnClass`, `@ConditionalOnBean` and `@ConditionalOnProperty`, and import
auto-configurations with `@ImportAutoConfiguration(classes = [...])`; no Spring Boot application
runs. The rules, in the `surf-api-components` skill:

- `@ConditionalOnProperty` reads system properties and environment variables only.
- `@ConditionalOnMissingBean` on a component-scanned class fails the plugin's start, because the
  condition would always remove the class. Annotate the class with `@Fallback` or declare it from
  a `@Bean` method.
- Classes annotated with `@AutoConfiguration` are never component-scanned; import them.
- A failed start that Spring Boot's failure analyzers recognize carries their description and
  action in a `ContextFailureAnalyzedException` inside the `PluginContextException`.
- `surf-api-standalone-testing` adds `surfContextRunner()` and brings `spring-boot-test` and
  AssertJ.

### Standalone programs

`spring-boot` on a standalone program's class path turns off log4j's own shutdown hook. The
shutdown hook of `SurfApiStandaloneBootstrap.enable()` shuts log4j down after surf-api; a program
that passes `registerShutdownHook = false` and uses log4j calls `LogManager.shutdown()` itself.

`SurfApiStandaloneBootstrap.bootstrap()` takes an optional `application` descriptor; with one, the
root context also hosts the application's beans. `SurfApiStandaloneBootstrap.adopt(context,
application)` makes a Spring Boot program's own context surf-api's root context.

surf-api's own root components without an explicit bean name are named by their fully qualified
class name. An application's components keep Spring's short names, so a class of the same simple
name, such as `UuidSerializer`, no longer collides with surf-api's. Inject surf-api's beans by type
rather than by a derived bean name.

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

### `@Scheduled` runs in the plugin's scope

Every plugin context processes Spring's `@Scheduled` methods without `@EnableScheduling`, so
repeating Bukkit or mccoroutine tasks become annotated bean methods. A plugin that already declares
`@EnableScheduling` keeps working; surf-api replaces the processor it declares with its own. The
behaviour differs from plain Spring:

- Tasks start after the `onEnable` hooks, not when the context refreshes, and stop before the first
  `onDisable` hook.
- Every task runs inside the plugin's `CoroutineScope`. A method without `scheduler` runs on the
  scope's dispatcher, `Dispatchers.Default`, not on a `taskScheduler` bean or Spring's
  single-threaded fallback. `scheduler` selects `SurfSchedulers.DEFAULT` or `SurfSchedulers.IO`
  (`dev.slne.surf.api.spring`), or on Paper `PaperSchedulers.GLOBAL_REGION`
  (`dev.slne.surf.api.paper.coroutine`) for the server thread. A `TaskScheduler` bean the plugin
  declares is used only when `scheduler` names it, and is marked `@Primary` if the plugin injects
  `TaskScheduler` unqualified.
- Methods may suspend and need no Reactor on the class path. Such a method takes no parameters. Its
  `fixedDelay` waits for the run to complete, and its `fixedRate` skips the starts a long run missed
  and never overlaps runs.
- A `fixedRate` method that does not suspend also skips missed starts instead of catching up, and a
  failing run is logged without ending the task.

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

A plugin declares a serializer for its own types as a `@SurfConfigSerializer` class, which becomes
a bean of the plugin (recipe 6); `surfSpongeConfigSerializers`, `SpongeConfigSerializers` and
`ConfigSerializerRegistrar` are removed. `@SurfConfig(lazy = true)` defers reading the file to the
first access of its handle.

#### Removed configuration API

| Removed | Replacement |
|---|---|
| `SpongeYmlConfigClass`, `SpongeJsonConfigClass` and their base `SpongeConfigClass` (`getConfig`, `edit`, `save`, `reloadFromFile`, `init`, `isInitialized`, `manager`, `migration`, `versionKey`) | `@SurfConfig` with an injected `ConfigHandle`, `configHandle<T>()` or `by surfConfig<T>()`; `ConfigHandles.open` for other files. Recipes below. |
| `SurfConfigApi`: `createSpongeYmlConfig`, `createSpongeJsonConfig`, `createSpongeYmlConfigManager`, `createSpongeJsonConfigManager`, `getSpongeConfig`, `reloadSpongeConfig`, `getSpongeConfigManagerForConfig` and their reified extensions | `ConfigHandles.open<T>(path, format, migrations)`. Nothing tracks configs by class any more; keep the returned handle. |
| `ConfigMigrationBuilder`, `MigrationResult` | `ConfigMigrations` and `configMigrations<T> { }` |
| `SpongeConfigManager` | Internal (`@InternalSurfApi`); use `ConfigHandle` |
| `SpongeConfigSerializers.ComponentSerializer`, `SpongeConfigSerializers.LinkedListSerializer` | Internal; every handle still registers both, so `Component` and `LinkedList` fields keep working |
| `SpongeConfigSerializers` with `registerClassSerializer`, `registerTypeTokenSerializer`, `unregisterSerializer`, `unregisterTypeTokenSerializer`, `buildSerializersModule`, `classSerializers` and `typeTokenSerializers`; its platform subclasses `PaperSpongeConfigSerializers`, `VelocitySpongeConfigSerializers`, `MinestomSpongeConfigSerializer` and `StandaloneSpongeConfigSerializers` | A `@SurfConfigSerializer` class per serializer (recipe 6). Nothing is registered at runtime any more, and nothing can be unregistered. |
| `ConfigSerializerRegistrar`, which earlier 4.0 snapshots offered | A `@SurfConfigSerializer` class per serializer (recipe 6) |
| `DoubleOr.Disabled.Serializer` | Internal, like the serializers of the other helper types; they still apply to every config file |
| `YamlConfigFileNamePattern`, `JsonConfigFileNamePattern` | Nothing; `ConfigHandles.open` takes any path |
| The fallback that read config classes annotated with Configurate's original `org.spongepowered.configurate` packages | Nothing. surf-api still relocates Configurate to `dev.slne.surf.api.libs.configurate`, and the surf-api Gradle plugins relocate it in a plugin's jar as well. Opening a config class whose annotations were not relocated fails with a `LoadConfigException` that names them. |
| `DatabaseConfig.load(configApi, directory, fileName)`, `SurfDatabaseAutoConfiguration.surfDatabaseConfig(configApi, environment)` | `DatabaseConfig.load(directory, fileName)`, `surfDatabaseConfig(environment)` |

Behaviour changes:

- `ConfigHandle.reload()` no longer writes the file back. It writes only when a migration changed
  the file, and it takes an optional `validate` check (recipe 5).
- `ConfigHandle.reload()` on an emptied file keeps the current value and logs a warning instead of
  installing the defaults. Opening a missing file still writes the defaults.
- `ConfigHandle` is `AutoCloseable`. A closed handle keeps its last `value` and rejects every other
  call. The handles of `@SurfConfig` classes close with their plugin's context.
- surf-redis, surf-nats and surf-database-r2dbc read their `config.yml` and `database.yml` once
  through `ConfigHandles.open`. The files and keys are unchanged.
- A plugin's serializers apply only to its own config files. Through `surfSpongeConfigSerializers`,
  every plugin registered into one shared registry, so a serializer of one plugin also applied to
  the files of all others.
- Among surf-api's own serializers, the one for the most specific type wins. A `NamespacedKey`
  field is now read by the `NamespacedKey` serializer instead of the `Key` serializer, and a
  `Material` field by the `Material` serializer, which writes the namespaced key such as
  `minecraft:stone` and reads legacy names as well.
- On Paper, a `@ConfigSerializable` class that also implements Bukkit's `ConfigurationSerializable`
  is now written through Bukkit's serialization instead of Configurate's object mapper, because
  every root default, the `ConfigurationSerializable` one included, is asked before the object
  mapper. Its file format changes accordingly. To keep the object-mapper format, name a serializer
  for the class in `@SurfConfig(serializers = [...])` or drop the `ConfigurationSerializable`
  interface.
- `ConfigHandle` gained `load()`. A class that implements `ConfigHandle` itself implements it too.

#### Recipe 1: static access through a companion

Before:

```kotlin
@ConfigSerializable
data class ShopConfig(val discount: Double = 0.0) {
    companion object : SpongeYmlConfigClass<ShopConfig>(
        ShopConfig::class.java, plugin.dataPath, "shop.yml"
    )
}

val discount = ShopConfig.getConfig().discount
```

After: declare the file, then inject the handle into beans. Code that is not a bean reads it
statically; surf-api finds the plugin's context through the class loader of `ShopConfig`:

```kotlin
@ConfigSerializable
@SurfConfig("shop.yml")
data class ShopConfig(val discount: Double = 0.0)

object Prices {
    private val config by surfConfig<ShopConfig>()

    fun price(base: Double) = base * (1 - config.discount)
}
```

Every read of the delegate returns the current value. `configHandle<ShopConfig>()` returns the
handle itself, for `edit`, `update`, `save` and `reload`. Both work from the moment the plugin's
context starts, bean constructors included, and throw `IllegalStateException` before that and after
the context closed.

#### Recipe 2: a path known only at runtime

Before:

```kotlin
val manager = configApi.createSpongeYmlConfigManager<ArenaConfig>(arenaFolder, "arena.yml")
val arena = manager.config
```

After:

```kotlin
ConfigHandles.open<ArenaConfig>(arenaFolder.resolve("arena.yml")).use { handle ->
    val arena = handle.value
}
```

The caller owns the handle: keep it open while it reads or edits the file, close it afterwards.
Without a format, a `.json` file opens as JSON and any other as YAML; pass a `ConfigFormat` to
override the extension.

#### Recipe 3: loading before the plugin's context exists

Before: `MyConfig.init()` in `onLoad`, or a `Condition` that reads `MyConfig.getConfig()`.

After: a `@SurfConfig` file is loaded while the plugin's context starts, before any `onLoad` hook,
so `init()` calls simply go. Code that runs earlier, such as a Spring `Condition` evaluated during
the component scan, opens the file itself:

```kotlin
val earlyConfig: ConfigHandle<MyConfig> by lazy {
    ConfigHandles.open(plugin.dataPath.resolve("my-config.yml"))
}
```

surf-api's root context must be running, because the serializers come from it. On Paper it starts
in the load phase, so a bootstrapper cannot read a config through surf-api.

#### Recipe 4: lambda migrations and a custom version key

Before:

```kotlin
companion object : SpongeYmlConfigClass<MyConfig>(MyConfig::class.java, folder, "my.yml") {
    init {
        versionKey("meta", "version")
        migration(1, RenameServerVersion)
        migration(3) { node -> node.node("legacy").raw(null) }
    }
}
```

After: declare a `ConfigMigrations<MyConfig>` bean in the plugin. Versions stay explicit and may
leave gaps:

```kotlin
@ConfigSerializable
@SurfConfig("my.yml")
data class MyConfig(val release: String = "1.0.0")

@Configuration
class MyConfigBeans {

    @Bean
    fun myConfigMigrations() = configMigrations<MyConfig> {
        versionKey("meta", "version")
        version(1, RenameServerVersion)
        version(3) { node -> node.node("legacy").raw(null) }
    }
}
```

A root-level version key also fits into the annotation: `@SurfConfig("my.yml", versionKey =
"version")`. Migrations go either into `@SurfConfig(migrations = [...])`, numbered by position, or
into the bean, and so does the version key; declaring either in both places fails the plugin's
start. For `ConfigHandles.open`, pass the same
object as `migrations`.

#### Recipe 5: reloading with validation

Before:

```kotlin
val reloaded = MyConfig.reloadFromFile()
check(reloaded.port in 1..65535)
```

The file was rewritten and the new value installed before the check ran.

After:

```kotlin
config.reload { candidate -> check(candidate.port in 1..65535) { "Invalid port" } }
```

`reload` reads the file into a candidate, migrates it in memory and installs it only when
`validate` returns. If `validate` throws, the old value and the file stay untouched and the
exception propagates.

#### Recipe 6: a serializer for your own type

Before:

```kotlin
surfSpongeConfigSerializers.registerClassSerializer<Price>(PriceSerializer)
```

After: annotate the serializer itself. It becomes a bean of the plugin, so it may inject other
beans, and applies to every config file of the plugin:

```kotlin
@SurfConfigSerializer
class PriceSerializer : ScalarSerializer<Price>(Price::class.java) {
    override fun deserialize(type: Type, obj: Any): Price = Price.parse(obj.toString())

    override fun serialize(item: Price, typeSupported: Predicate<Class<*>>): Any = item.toString()
}
```

- The type comes from the serializer's `TypeSerializer<T>` supertype, generics included, or from
  `type()` of a `ScalarSerializer`. Where it cannot be resolved, name it:
  `@SurfConfigSerializer(type = Price::class)`.
- `@SurfConfigSerializer(applyToAll = false)` limits the serializer to the files that name it:
  `@SurfConfig("shop.yml", serializers = [PriceSerializer::class])`.
- A file asks its named serializers first, then the plugin's `applyToAll` serializers, then
  surf-api's defaults. A plugin serializer for `UUID` therefore replaces surf-api's within the
  plugin. Two `applyToAll` serializers of one plugin for the same type fail its start.
- A Kotlin `object` serializer becomes a `class`: Spring creates the instance.
- For `ConfigHandles.open`, pass the instances:
  `ConfigHandles.open<ShopConfig>(path, serializers = listOf(PriceSerializer()))`.

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

### PlaceholderAPI expansions

On Paper, a class annotated with `@PapiExpansion` (`dev.slne.surf.api.paper.hook.papi`) is a
component that exists only while PlaceholderAPI is installed. surf-api registers it with the
plugin's enable phase and unregisters it on disable. Its `@Placeholder` methods replace
`PapiPlaceholder` subclasses, and `PapiExpansionFallback` replaces `parseWithNoParams`.

Before:

```kotlin
class CoinsPlaceholder : PapiPlaceholder("coins") {
    override fun parse(player: OfflinePlayer, args: List<String>) = coins(player).toString()
}

@Service
class CoinsExpansion(private val papiHook: SurfPaperPAPIHook) : SurfLifecycle {
    private val expansion = PapiExpansion("example", listOf(CoinsPlaceholder()))

    override suspend fun onEnable() = papiHook.register(expansion)
    override suspend fun onDisable() = papiHook.unregister(expansion)
}
```

After:

```kotlin
@PapiExpansion("example")
class CoinsExpansion(private val coins: CoinService) {

    @Placeholder("coins")
    fun coins(player: OfflinePlayer?): String? = player?.let { coins.balance(it).toString() }
}
```

A `@Placeholder` method takes `OfflinePlayer?`, `OfflinePlayer` or `Player`, optionally followed by
the arguments as one `String`, and returns `String?` or `Component?`. A non-null `OfflinePlayer`
skips requests without a player, and `Player` skips players that are offline; both resolve those to
`null`. The author and version default to the plugin's. A class listed in `placeholders` is used as
its Kotlin `object`, as its bean in the plugin's context, or created through the context and
destroyed on disable; several matching beans fail the plugin's enable.

The hand-built `PapiExpansion` class and `SurfPaperPAPIHook` still work, with these changes:

- `%identifier_%` now reaches `parseWithNoParams`; before, it never did.
- A request without a player resolves to `null`; before, it threw.
- Identifiers and placeholder names must not be empty, also reject `}`, and identifiers are unique
  ignoring case.
- `register` throws `IllegalStateException` when PlaceholderAPI rejects the expansion; before,
  the rejection was ignored.
- `unregister` removes only the instance that was registered, not any expansion with the same
  identifier.

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
runs its Java implementation. Code that used `org.bouncycastle` classes through a surf-nats jar
declares BouncyCastle itself. On Minestom, `io.nats` and BouncyCastle stay unrelocated.

**No `-all` variants.** The surf-api modules built on surf-api's own conventions (every api, impl and
aggregate module outside `infra/`) do not publish a `shadowRuntimeElements` variant, so no
coordinate such as `dev.slne.surf.api:surf-api-core-api:4.0.0:all` exists. Depend on the plain
artifact instead of the `all` classifier or `Bundling.SHADOWED`. The shaded runtime jars of
`surf-api-paper-impl`, `surf-api-velocity-impl` and `surf-api-standalone-impl` are attached to the
GitHub release; their plain artifacts stay in the Maven repository. The `shadow` publications of
the `infra/` libraries, such as the `-all` jars of `surf-api-redis-api` and
`surf-api-redis-spring`, are unchanged.

**Third-party libraries are compile-only.** The api modules, and with them `surf-api-core`,
`surf-api-paper`, `surf-api-velocity` and `surf-api-minestom`, declare the Kotlin stdlib,
kotlin-reflect, kotlinx-coroutines, kotlinx-serialization, Adventure, Adventure NBT, Configurate,
Spring, glm, aedile, inventory-framework, the CommandAPI Kotlin DSLs, wyck and the utility
libraries (Guava, dazzleconf, the Commons libraries, flogger, spongepowered math) as
`compileOnlyApi`. A plugin still compiles against them, but they no longer reach its runtime
classpath: the surf-api runtime on the server ships them.

- A test that runs plugin code without a server would lose these libraries at runtime. The surf
  Gradle plugins add them to `testRuntimeOnly`: `surf-api-standalone` for
  `dev.slne.surf.api.gradle.core` projects, and for Paper, Velocity and Minestom plugin projects the
  libraries the platform's surf-api runtime ships, minus those its server API already brings. Delete
  `testRuntimeOnly` lines that only declared such libraries. Velocity plugin tests also get
  `velocity-api` on `testImplementation`, as Paper plugin tests get `paper-api`.
- Only Gradle builds can consume surf-api. `compileOnlyApi` exists only in Gradle module metadata;
  the Maven POMs leave these libraries out, so a Maven build cannot compile against surf-api.
- `surf-api-standalone` and `surf-api-minestom-server` keep shipping everything at runtime.
- `kotlinx-coroutines-reactor`, `kotlinx-coroutines-reactive` and `reactor-core` left the api
  modules' compile classpath as well. `surf-api-redis-api`, whose types extend Reactor's, declares
  them itself; other code that uses them declares them with `compileOnly`. The surf-api runtime
  still ships them.

**Exposed and r2dbc-spi are dependencies.** The `surf-api-database-r2dbc` jar no longer bundles
Exposed or `io.r2dbc:r2dbc-spi`; it declares both as dependencies, under their own packages, and
Exposed brings `kotlinx-datetime`. The PostgreSQL R2DBC driver stays bundled but keeps its own
package `io.r2dbc.postgresql`, because Exposed refers to its JSON codec. The R2DBC pool, the
MariaDB driver, the JDBC drivers, Netty and Flyway stay bundled and relocated below
`dev.slne.surf.database.libs`. Rewrite the imports:

```kotlin
// before
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import dev.slne.surf.database.libs.io.r2dbc.spi.IsolationLevel
// after
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import io.r2dbc.spi.IsolationLevel
```

`MicroserviceReliabilityAutoConfiguration.atomicOperations` takes the unrelocated
`org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase`. A project that declares Exposed or `r2dbc-spi`
itself shares the library's copy; Gradle picks one version. Do not add
`org.postgresql:r2dbc-postgresql`: the bundled copy conflicts with a second one.

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
| `SurfTypeParameterMatcher` | Spring's `GenericTypeResolver.resolveTypeArgument(type, Generic::class.java)`, or `ResolvableType` for several type parameters; then `isInstance` on the resolved class |
| `mockBukkit` and `mockBukkitVersion` in the `testing { }` block of the surf Gradle plugins | Nothing; surf-api no longer tests against MockBukkit. Mock the few Bukkit statics a test needs with mockk (`mockkStatic(Bukkit::class)`). |
| `dev.slne.surf.api.shared.api.annotation.AnnotationUtils` (`findAnnotation`, `clearCache`) | Spring's `org.springframework.core.annotation.AnnotationUtils.findAnnotation(type, annotationType)`, which searches the same places and caches; `MergedAnnotations` when attributes or `@AliasFor` matter. |
| `IntOr.Disabled.enabled()`, `DoubleOr.Disabled.enabled()` | `isEnabled()` |
| `SurfVisualizer.visibleTo(player)` | `isVisibleTo(player)` |
| `SystemChatPacket.overlay` (`getOverlay`) | `isOverlay` (`isOverlay()`); `setOverlay` is unchanged |
| `LastSeenMessagesValidatorMirror.LastSeenTrackedEntry.pending` | `isPending`; the serialized field is still named `pending` |
| Minestom `ParsedArgument.present`, `ArgumentDefinition.greedy` and the `Argument.greedy` override point | `isPresent`, `isGreedy` |
| `NatsEventDeduplicator.firstDelivery(eventId)` and `firstDelivery()` | `isFirstDelivery(eventId)` and `isFirstDelivery()` |
| `SurfComponentBuilder` as an interface | An abstract class that only surf-api implements. Builder blocks and calls are unchanged; a class of your own that implements it no longer compiles. |
| The colour interfaces in `dev.slne.surf.api.messages.builder` and `.builder.colors` (`ComponentBuilderColor`, `ComponentBuilderColors`, `SurfComponentBuilderColors`, `MinecraftComponentBuilderColors` and the 26 `*ComponentBuilderColor` interfaces) | Plain members of `SurfComponentBuilder`: `primary(...)`, `red(...)`, `appendErrorPrefix()` and the rest. Calls inside a builder block are unchanged; delete imports of these interfaces. |
| `coloredComponent(value, color, ...)` | `colored(value, color, ...)` |
| `SurfComponentBuilder.note(any: Any, ...)` | `note(...)` with a `String`, `Boolean`, `Char` or `Number`, like the other colour members. Calls with one of these types compile unchanged; pass any other value as `note(value.toString())`. |
| `appendAsync { }`, `appendNewlineAsync { }`, `appendNewInfoPrefixedLineAsync { }` and the success, warning and error variants | `append { }`, `appendNewline { }`, `appendNewInfoPrefixedLine { }` and so on. They are `inline`, so the block may suspend. |
| `CommonComponents.renderKickDisconnectMessage` and `renderDisconnectMessage` overloads that take a builder (generic `B : TextComponent.Builder` or `SurfComponentBuilder`) | `builder.appendKickDisconnectMessage(...)` and `builder.appendDisconnectMessage(...)`, or the overloads without a builder. |
| `TextComponent.Builder` as the receiver of the `renderKickDisconnectMessage` and `renderDisconnectMessage` blocks | `SurfComponentBuilder`. A bare `text("...", color)` in such a block now appends the text; before, it created a component and discarded it. |
| Style calls (`color`, `decorate`, `clickEvent`, `hoverEvent`, `style`) on the receiver of a `renderKickDisconnectMessage` or `renderDisconnectMessage` block styled the whole message | They style only the block's own part, which is appended as a child component. Style the whole message on the returned component. `appendKickDisconnectMessage` and `appendDisconnectMessage` now write header, headline, message and footer directly into the outer builder instead of one child component, so style that builder to style the whole message. |
| Paper: `suspend fun OfflinePlayer.getLuckPermsUser()` | `OfflinePlayer.loadLuckPermsUser()`; Velocity gains `Player.loadLuckPermsUser()`. |
| File facades `UtilBukkit.getLuckPermsUser`, `getLuckPermsUserOrNull`, `getPrefixedName` (Paper) and `LuckPermsAccessKt` (the `User` and `Audience` extensions) | `UtilLuckPerms` and `LuckPermsExtensions`. Kotlin callers only recompile. The message of `Audience.getLuckPermsUser()` for a missing user changed. |
| `CompoundBinaryTag.fast(synchronize = true)`, `buildFast(synchronize = true)` | `fast(concurrent = true)`, `buildFast(concurrent = true)`. A concurrent tag is backed by a `ConcurrentHashMap` instead of a synchronized map, and `keySet()` of every tag is a read-only view. |
| `dev.slne.surf.api.paper.command.util.idOrThrow` (`PlayerProfile.idOrThrow()`) | `dev.slne.surf.api.paper.util.idOrThrow`; only the import changes. The facade class changed from `AsyncPlayerProfileArgumentUtilsKt` to `UtilPlayerProfile`. |
| `SurfCommandUtil.failWithMessage(...)` and `failWithBuilder(...)` returned `Unit` | They return `Nothing`, so Kotlin code after the call is unreachable and the call ends a branch. Java sees `Void` instead of `void`; a call used as a statement compiles unchanged. |
| NATS health indicator (`nats`): up with the detail `connection: none` while the context has no connection | `UNKNOWN` with the detail `connection: no client` while the context has no `NatsClient`; `DOWN` while a client exists and the connection is not `CONNECTED`, including before it connected. `microservicectl health` now reports only `DOWN` and `OUT_OF_SERVICE` as unhealthy, so a microservice that uses nothing of NATS stays healthy. |
| `NmsProvider`, `NmsProvider.current`, `@NmsProviderMarker`, `NmsProviderMeta`, `NmsProviderConfig` and the twenty bridge proxies in `dev.slne.surf.api.paper.server.proxy` | Inject the bridge interface (`SurfPaperNmsItemBridge`, `SurfGlowingApi`, `TickThreadGuard`, ...). Each NMS version module contributes its implementations as beans through a configuration annotated with `@ConditionalOnMinecraftVersion`. |
| `NmsVersionResolver.current`, `NmsVersion.NEWEST` | `NmsVersionResolver.resolve(minecraftVersion)`, or inject the selected `NmsVersion` bean. |
| NMS version fallback: a server without bindings of its own version ran on the newest bindings | It runs on the newest bindings not newer than the server; a server older than every supported version fails surf-api's bootstrap. |
| `byIdMap`, `byByteIdMap`, `byStringIdMap` and `byEnumMap` (facade `SurfUtil`) | The standard library: `values.associateBy { it.id }`, `enumEntries<E>().associateBy(...)`, `associateWith(...)`. |
| `setFinalField`, `setStaticFinalField` (facade `SurfUtil`) | Nothing; surf-api no longer writes `final` fields. |
| `ById`, `ById.ByByteId`, `ById.build(enumClass)`, `ByEnum`, `ToByteFunction` in `dev.slne.surf.api.common.util` | Nothing. Declare the interface in your project; replace `ById.build(E::class.java)` with `enumEntries<E>().associateBy { it.id() }`. |
| `Colors.SPACER`, `DARK_SPACER` and the 16 Minecraft colours (`BLACK` to `WHITE`) typed `NamedTextColor` | Typed `TextColor`, same values. A `NamedTextColor` variable or parameter fed from them no longer compiles; declare `TextColor`, or use `NamedTextColor.RED` and so on. |
| `Rarity.displayName` and `Rarity.asComponent()` returned `TextComponent` | They return `Component`. |
| `LazyIntList(defaultValueGenerator: () -> Int)` | `LazyIntList(IntSupplier)`. A lambda (`LazyIntList { 0 }`) still compiles; a stored `() -> Int` needs `LazyIntList(generator::invoke)`. A negative index throws `IllegalArgumentException`. |
| `ComponentSqlLogger(level = ...)` with a logger named after the calling class | Pass the logger: `ComponentSqlLogger(ComponentLogger.logger("MyService"), level)`. |
| `dev.slne.surf.api.messages.ComponentMessage` (a Brigadier `Message` whose `getString()` returned a UUID prefix plus Gson JSON that only Velocity decoded; Paper showed the raw text) | Nothing. Fail a command with `SurfCommandUtil.failWithMessage(component)`; `WrapperCommandExceptionComponent.component` returns the original component. Minestom keeps `dev.slne.surf.api.minestom.command.exception.ComponentMessage` and `CommandAPI.failWithMessage`. |
| `SurfStandaloneApiImpl.dataFolder` (`api-data`) | Nothing; choose the data directory of your program yourself. |

### Nested values in the kotlinx serializers resolve through the serializers module

The serializers of `surf-api-serialization-api` and the Paper serializers write a nested UUID,
`Component`, `Key`, `URI`, `BigInteger`, `InetAddress`, `LocalDate` or `LocalTime` through a
`ContextualSerializer`. The surf-api serializer stays the fallback, so a format without a matching
contextual serializer writes the same as in 3.x. A contextual serializer in the format's module now
wins.

**Redis JSON changes shape.** surf-redis registers `JavaUUIDStringSerializer` for `UUID`, so a UUID
nested in `PlayerSerializer`, `OfflinePlayerSerializer`, `LocationSerializer` or
`AdventureResourcePackInfoSerializer` is now a string where it was a byte array:

```json
// before
{"world_uuid":[0,17,34,51,68,85,102,119,-120,-103,-86,-69,-52,-35,-18,-1],"x":1.0,...}
// after
{"world_uuid":"00112233-4455-6677-8899-aabbccddeeff","x":1.0,...}
```

Values written by 3.x cannot be read back; there is no tolerant decoder. Clear or rewrite such keys
when you upgrade.

Further serializer changes:

- `SurfPaperSerializerModule.bukkit` registers the Location, ItemStack, OfflinePlayer and BlockData
  serializers as contextual serializers; `SurfPaperSerializerModule.all` adds
  `SurfSerializerModule.all`.
- `AdventureTextColorSerializer` rejects a value outside `0` to `0xFFFFFF` with a
  `SerializationException`; it used to truncate it.
- New: `BigDecimalStringSerializer` (`BigDecimal.toString()`) and `InetAddressBytesSerializer` (the
  raw 4 or 16 address bytes).
- The `surf-api-nats-types` serializers for single-field messages (`UuidSerializer`,
  `ComponentSerializer`, `KeySerializer`, `TextColorSerializer`, `ShadowColorSerializer`,
  `IpAddressSerializer`, `BigIntegerSerializer`, `BigDecimalSerializer` and the Paper
  `ItemStackSerializer`) wrap the surf-api serializers in `ProtoWrapperSerializer`. Their names
  and wire format are unchanged.

### Reflection proxies replace `SurfReflection` and `@GenerateReflection`

surf-api has one reflection front-end. A `@ReflectionProxy` interface names its target class, and
each abstract method binds one member of it. surf-api implements the interface at runtime as a
hidden class that calls constant method handles; nothing is generated at compile time.

```kotlin
// before
@SurfProxy(qualifiedName = "org.spigotmc.RestartCommand")
interface RestartCommandProxy {
    @Static
    fun restart()
}
val proxy = surfReflection.createProxy<RestartCommandProxy>()

// after
@ReflectionProxy(targetName = "org.spigotmc.RestartCommand")
interface RestartCommandProxy {
    @ReflectedStatic
    @ReflectedMethod
    fun restart()
}
val proxy = reflectionProxy<RestartCommandProxy>() // or inject RestartCommandProxy
```

| Removed | Replacement |
|---|---|
| `SurfReflection`, `createProxy<T>()`, `createProxy(clazz, classLoader)` | `reflectionProxy<T>()`, `ReflectionProxies.get(type)`, or inject the interface: every plugin context registers a lazy bean for each `@ReflectionProxy` interface in the plugin's packages. |
| `@SurfProxy(value)`, `@SurfProxy(qualifiedName = …)` | `@ReflectionProxy(target)`, `@ReflectionProxy(targetName = …)` |
| `@Field(name, Field.Type.GETTER)`, `@Field(name, Field.Type.SETTER)` | `@ReflectedField(name)`, `@ReflectedField(name, FieldAccess.SET)` |
| `@Field(overrideFinal = true)` | Nothing. Writing a `final` field fails when the proxy is created. |
| `@Constructor` | `@ReflectedConstructor` |
| `@Static` | `@ReflectedStatic` next to `@ReflectedMethod` or `@ReflectedField` |
| `@Name("…")` | The `name` of `@ReflectedMethod` or `@ReflectedField` |
| A method without annotation, bound to the method of the same name | `@ReflectedMethod`. Every abstract method needs exactly one of `@ReflectedMethod`, `@ReflectedField` and `@ReflectedConstructor`. |
| `@GenerateReflection`, `generatedReflectionAccessor<T>()`, `GeneratedReflectionAccessors` and the `surf-api-processor` code generation behind them | A `@ReflectionProxy` interface per target class; `companion object : X by reflectionProxy<X>()` keeps static call sites. |
| `@ReflectedMethod`, `@ReflectedField`, `@ReflectedConstructor` in `dev.slne.surf.api.shared.api.reflection` with `target`, `targetName`, `isStatic`, `descriptor`, `type`, `invocation` | The annotations of the same name in `dev.slne.surf.api.reflection`. The target moves to `@ReflectionProxy`, `isStatic` becomes `@ReflectedStatic`, and members are matched by name and parameter types. |
| `@ReflectedVarHandle` | A `@ReflectedField` property that returns a field handle: `IntFieldHandle<R>`, `LongFieldHandle<R>`, `BooleanFieldHandle<R>`, `ObjectFieldHandle<R, T>`, or with `@ReflectedStatic` `StaticIntFieldHandle`, `StaticLongFieldHandle`, `StaticBooleanFieldHandle`, `StaticObjectFieldHandle<T>`. Each operation calls the `VarHandle` access mode of its name (`getVolatile`, `setRelease`, `compareAndSet`, `getAndAdd`, `getAndBitwiseOr`, …); `getAndIncrement`, `addAndGet` and the other numeric shortcuts build on `getAndAdd`. There are no `float`, `double`, `short`, `byte` or `char` handles. |
| `@ConstantIntArgument` and the other constant-argument annotations, `ReflectionInvocationMode`, `ReflectedFieldAccess`, `UnspecifiedReflectionTarget` | Nothing. Read and write the field with `@ReflectedField`, or pass the constant at the call site. |
| `xyz.jpenilla:reflection-remapper` on the Paper compile classpath and the `ReflectionProxyFactory.reflectionProxy<I>()` extension | Nothing; Paper is Mojang-mapped. Declare the dependency yourself if you still use it. |

```kotlin
@ReflectionProxy(ServerGamePacketListenerImpl::class)
interface PacketListenerReflection {
    @ReflectedField("nextChatIndex")
    val nextChatIndex: IntFieldHandle<ServerGamePacketListenerImpl>
}

val index = reflectionProxy<PacketListenerReflection>().nextChatIndex.getAndIncrement(listener)
```

A proxy is created once per interface and validates every method at creation: a member that does
not exist, an ambiguous overload, a `suspend` method, a `final` write, a field handle on a `final`
field or a field handle whose type does not fit the field throws one `ReflectionProxyException`
listing every problem. Calls propagate the member's exceptions
unchanged instead of wrapping them in `ProxyInvocationException`.

The hidden-class invoker infrastructure in `dev.slne.surf.api.common.invoker` was rewritten on the
ClassFile API and is internal (`@InternalSurfApi`) now. `HiddenInvokerUtil`, `InvokerClassData`,
the template-class `InvokerFactory`, `SuspendInvokerSupport` and the `@InternalInvokerApi`
opt-in are removed. There is no public replacement: a library that defined its own invoker
templates generates its own hidden classes or binds its handles with `LambdaMetafactory`.

### Java consumers are no longer supported

surf-api 4.0 targets Kotlin consumers built with Gradle. Java plugins and Java sources in a plugin
are not supported, and the api no longer generates declarations whose only purpose is calling it
from Java. Kotlin source compiles unchanged; the Kotlin call resolves to the same declaration.

- **`@JvmStatic` bridges removed:** `ConfigHandles.open`, `ConfigFormat.ofPath`,
  `EnvironmentVariables.from` (both overloads), `ConfigMigrations.builder` and `none`,
  `SurfComponentBuilder.builder`, `ReflectionProxies.get`, `SurfCommandUtil.createException`,
  `failWithMessage` and `failWithBuilder`, `RedisUtf8String.read` and `write`, and the
  Minestom implementation's `CommandAPIHook.reportUnknown`. Call them through the companion or the
  `object`, as Kotlin always did.
- **`@JvmOverloads` removed:** `ConfigHandles.open`, `RandomSelector.fromFlow`, `fromIterable` and
  both `fromWeightedIterable` factories, and the constructors of `SurfMessageBundle`, `PapiExpansion`
  and `RedisCodecException`. Default arguments still work from Kotlin.
- **`ConfigHandles.open` picks the format from the extension.** Its `format` parameter is
  `ConfigFormat?` and defaults to `null`: `.json` opens as JSON, every other extension as YAML. A
  `.json` file opened without a format was read as YAML before.
- **Generated NATS enums:** the `forNumber` function of an enum that the surf-nats Gradle plugin
  generates is no longer `@JvmStatic`. Kotlin calls `Enum.forNumber(n)` as before.
- **Java API sources became Kotlin.** `RedisVarInt` and `RedisVarLong` are `object`s, and
  `UUIDCodec.INSTANCE` lives in the companion of `UUIDCodec`; Kotlin calls compile unchanged.
- **The NMS packet listeners are Kotlin interfaces.** `NmsPacketListener`,
  `NmsClientboundPacketListener` and `NmsServerboundPacketListener` keep their package, type
  parameter and handler methods, but a lambda no longer converts to them:
  `NmsClientboundPacketListener<P> { packet, player -> ... }` fails to compile. Such a lambda never
  received packets, because the packet type is resolved from the type argument of the implementing
  class. Implement the interface in a class or `object` instead. The internal `getPacketClass()`
  member is gone.
- **`@SurfConfig.versionKey` defaults to empty.** Empty leaves the key to a `ConfigMigrations<C>`
  bean, or to `_config_version`. A version key declared both in `@SurfConfig` and in the bean's
  builder now fails the plugin's start instead of letting the bean win.

`@JvmField` constants and `@file:JvmName` facades stay where they shape the bytecode, for example
the registry-key fields and the `UtilBukkit` facade.

## 21. Checklist for a downstream repository

1. Replace any coordinate from sections 3 and 4; the others keep working.
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
12. Replace every `SpongeYmlConfigClass` / `SpongeJsonConfigClass` companion and `SurfConfigApi` call
    with `@SurfConfig` or `ConfigHandles.open`, following the recipes in section 17, and turn
    every registered serializer into a `@SurfConfigSerializer` class (recipe 6).
13. Remove your own listener and command registration for beans that surf-api registers
    (section 17), and consider turning hand-built PlaceholderAPI expansions into `@PapiExpansion`
    classes.
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
19. Convert Java sources in your plugin to Kotlin; Java consumers are no longer supported
    (section 20).
20. Replace every `dev.slne.surf.database.libs.org.jetbrains.exposed` import with
    `org.jetbrains.exposed`, and give plugin tests that run without a server the libraries
    surf-api now only compiles against (section 19).
21. Turn every `@SurfProxy` and `@GenerateReflection` interface into a `@ReflectionProxy`
    interface and replace `createProxy` and `generatedReflectionAccessor` with
    `reflectionProxy<T>()` or injection (section 20).
22. Run `./gradlew check` and replace every internal API use that `checkInternalApiUsage` reports;
    pass your own internal markers to `withApiValidation(...)` (section 15).
