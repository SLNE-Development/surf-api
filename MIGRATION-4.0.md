# Migrating to surf-api 4.0.0

Written for the repositories that depend on surf-api — surf-transaction, surf-redis,
surf-rabbitmq, surf-nats and every plugin built with the surf Gradle plugins. It records what
changed and what each change asks of you, so a downstream repository can be made ready before
this branch is merged.

4.0.0 breaks compatibility on purpose. The major version is the whole point: the previous
release carried relocated shims, hidden overloads and a service-loader lookup that could not be
made to do what we need. Rather than add a third mechanism beside the two, this release removes
the old ones.

Nothing here is merged yet. The branch waits for the repositories in this list.

---

## 1. The group is unchanged

`dev.slne.surf.api`, as before. Only artifact ids and versions move.

## 2. Most consumers keep their coordinate

Every module was split into an `-api` part (contracts, what you compile against) and an `-impl`
part (what runs). To keep that split out of a consumer's build file, six **aggregate** artifacts
publish under the names the old modules had and export the parts:

| Keep writing | and you now get |
|---|---|
| `surf-api-core` | `surf-api-core-api` |
| `surf-api-paper` | `surf-api-paper-api` |
| `surf-api-minestom` | `surf-api-minestom-api` |
| `surf-api-velocity` | `surf-api-velocity-api` |
| `surf-api-minestom-server` | `surf-api-minestom-impl` |
| `surf-api-standalone` | `surf-api-standalone-impl` |

If your build file names one of these six, it needs no change for the rename.

> **Open question for reviewers.** The four `-api` aggregates export only the api part. If a
> downstream repository was relying on getting the implementation through `surf-api-paper` —
> because the old `surf-api-paper` module contained both — it will now fail to resolve a class at
> runtime rather than at compile time. Check your own repository against this before the merge
> and say so; it is cheaper to add the runtime export here than to find it on a server.

## 3. Coordinates that changed and are **not** aliased

These were renamed with no relocation POM behind them. A build file naming one of these fails to
resolve, which is the intended outcome — but it fails with "not found", not with a hint.

| Old artifact | New artifact |
|---|---|
| `surf-api-core-server` | `surf-api-core-impl` |
| `surf-api-paper-server` | `surf-api-paper-impl` |
| `surf-api-velocity-server` | `surf-api-velocity-impl` |
| `surf-api-paper-nms-common` | `surf-api-paper-nms-api` |
| `surf-api-shared-public` | `surf-api-annotations` |
| `surf-api-shared-internal` | `surf-api-internal` |

If you depended on any of these directly, you were depending on an implementation module. Prefer
the matching aggregate from the table above, and reach for an `-impl` coordinate only where you
genuinely need it.

## 4. The Minestom feature artifacts were renamed, with aliases

The eight Minestom server features moved from `surf-api-minestom-server-<feature>` to
`surf-api-minestom-<feature>-impl`, and each publishes a relocation POM, so an old coordinate
still resolves and Gradle tells you the new name:

`bootstrap`, `console`, `luckperms`, `npc`, `player-visibility`, `plugins`, `signed-chat`,
`spark`.

You do not normally name these at all — the `surfMinestomServer { }` Gradle block pulls in the
ones your `withXxx()` calls ask for.

## 5. New artifacts

| Artifact | What it is |
|---|---|
| `surf-api-registry-keys` | The four generated key classes (block types, item types, sounds, advancements), 40 848 lines that used to sit inside `surf-api-core` and only ever change with the Minecraft version. Pulled out so a core change does not recompile them. |
| `surf-api-di-api` | The dependency-injection contracts: `@Service`, `@Repository`, `@Bean`, `Scope`, `SurfComponent`, `BeanPostProcessor`, `BeanContainer`, the conditions. |
| `surf-api-di-impl` | The container itself. |
| `surf-api-bom` | A platform BOM listing every published artifact at one version. Prefer importing it over pinning versions one by one. |
| `surf-api-inventory-api` | The platform-neutral half of the inventory framework — view settings, header geometry, text alignment, the container component base and the DSL markers. Paper and Minestom each carried their own copy; see section 12. |
| `surf-api-common-api` | The shared base `surf-api-core` grew around: `util` (including `logger`), `collection`, `math`, `random`, `font`. Nothing in it names anything else of surf-api's, which is the point — every other domain can depend on it. |
| `surf-api-serialization-api` | The kotlinx-serialization serializers and codecs: `SurfSerializerModule`, the Adventure serializers, the JDK ones (UUID, dates, IP, numbers) and the SpongePowered math ones. |
| `surf-api-config-api` | The configuration surface: `SurfConfigApi`, the `SpongeYmlConfigClass` / `SpongeJsonConfigClass` companions, the 24 constraint annotations, the helper types, the managers and the migrations. |
| `surf-api-config-impl` | The config service behind `surfConfigApi`. |

**You do not name any of these in a build file.** Every coordinate that carried
these types before re-exports them, so `surf-api-core`, `surf-api-paper`,
`surf-api-minestom`, `surf-api-velocity`, `surf-api-standalone` and
`surf-api-minestom-server` all resolve them unchanged. What *did* change is which
package a type lives in — section 6.

## 6. `surf-api-core` was cut into domains, so some packages moved

`surf-api-core-api` had grown into a grab bag, and it is being cut domain by domain. The artifacts
are in section 5 and need no build-file change. The package prefixes do:

| Old package | New package |
|---|---|
| `dev.slne.surf.api.core.util.…` | `dev.slne.surf.api.common.util.…` |
| `dev.slne.surf.api.core.collection.…` | `dev.slne.surf.api.common.collection.…` |
| `dev.slne.surf.api.core.math.…` | `dev.slne.surf.api.common.math.…` |
| `dev.slne.surf.api.core.random.…` | `dev.slne.surf.api.common.random.…` |
| `dev.slne.surf.api.core.font.…` | `dev.slne.surf.api.common.font.…` |
| `dev.slne.surf.api.core.serializer.…` | `dev.slne.surf.api.serialization.…` |
| `dev.slne.surf.api.core.config.…` | `dev.slne.surf.api.config.…` |
| `dev.slne.surf.api.core.server.config.…` | `dev.slne.surf.api.config.impl.…` |

The subpackage and every type name are unchanged — only the prefix is. `logger()`, the single most
used of these, is now `dev.slne.surf.api.common.util.logger`.

**One type is not a prefix rewrite.** `TransformingObjectSet` declared
`dev.slne.surf.api.core.api.collection` while sitting in a `collection/` directory — a package that
matched no directory anywhere. It is now `dev.slne.surf.api.common.collection.TransformingObjectSet`,
with its directory. Nothing in surf-api used it.

There are no deprecated typealiases in the old packages: an import that names one fails to resolve,
which is the intended outcome. The compiler lists every one of them, so the migration is "compile,
follow the unresolved references" — and it is only `import` lines, never a build file.

## 7. fastutil is gone

**This is the change most likely to touch your code.**

surf-api no longer uses, exposes or exports [fastutil]. 213 type uses across 84 files were
replaced with their JDK equivalents, the 2 793-line helper bridge (`fast-util-util.kt`, 886
functions such as `toObjectList`, `toInt2ObjectMap`) was deleted, and 1 126 lines of fastutil
types left the published ABI.

It is also **no longer on your compile classpath through surf-api or the surf Gradle plugins**.
That was deliberate: the goal is that no repository picks fastutil up by accident.

What to do:

| If your code has | Write instead |
|---|---|
| `ObjectArrayList<T>`, `ObjectList<T>` | `ArrayList<T>`, `List<T>` |
| `Object2ObjectOpenHashMap`, `Object2ObjectMap` | `HashMap`, `Map` |
| `ObjectOpenHashSet`, `ObjectSet` | `HashSet`, `Set` |
| `Int2ObjectOpenHashMap` | `HashMap<Int, V>` |
| `IntArrayList`, `IntList`, `IntSet` | `ArrayList<Int>`, `List<Int>`, `Set<Int>` |
| `list.toObjectList()`, `map.toObject2ObjectMap()`, and the other 884 helpers | nothing — delete the call, the receiver already is the type you want |

If you have measured a hot path that genuinely needs a primitive-specialised collection, declare
`it.unimi.dsi:fastutil` in that module yourself. Nothing stops you; it simply is not inherited
any more.

### The one feature that is removed, not translated

`SpongeConfigSerializers` registered Configurate `TypeSerializer`s for **39 fastutil map types**.
Those registrations are gone. A `@ConfigSerializable` class with a field typed as a fastutil map
is no longer serialisable and will fail on load.

This is a real loss of function, not a renaming, so it is listed separately. It was also one of
the reasons for the decision: a config field had to be mapped back to a JDK collection to be
serialisable anyway, which is the detour that made the bridge cost more than it returned. Change
such a field to the JDK map — `Map<K, V>`, which Configurate has always handled.

### Where fastutil deliberately stays

Five modules keep it on the **compile** classpath only, and it appears in no signature and no
published artifact:

- the four `surf-api-paper-nms-v*` modules, for `CreativeOrderComparator`: one map is keyed
  through Paper's own `ItemStackLinkedSet.TYPE_AND_TAG` hash strategy and one through reference
  identity, and the JDK has no equivalent for either.
- `surf-api-minestom-impl`, because a mixin `@Shadow` of a Minestom field typed
  `Int2ObjectSyncMap` has to name that exact type to match.

[fastutil]: https://fastutil.di.unimi.it/

## 8. `requiredService` has been replaced by a container

The service-loader lookup (`requiredService<T>()`) is gone from surf-api's own services, replaced
by a dependency-injection container that works the way Spring's does: constructor injection,
scopes, conditions, an explicit lifecycle and post processors.

**This part of 4.0.0 is being built, and the container now runs.** The root container starts on
both platforms before anything else in surf-api loads, and all 40 of surf-api's own service
lookups resolve through it. The contracts are in `surf-api-di-api` and may still move in detail,
so the shape below is what to plan against rather than what to compile against today.

What this changes for you, concretely:

- **A surf-api service now resolves when you first use it, not when its class is loaded.** Every
  accessor you already call — `surfConfigApi`, `surfReflection`, `SurfScoreboardApi`,
  `surfSpongeConfigSerializers`, the NMS bridges — kept its name and its type. What changed is the
  moment behind it: it used to be a `ServiceLoader` lookup running at class initialisation, and it
  is now a lazy read of the container. Code that touched one of these during its own class
  initialisation, before surf-api had started, used to get a `ServiceConfigurationError`; it now
  gets a clear `IllegalStateException` naming the fix.
- **`@AutoService` is no longer how an implementation is published.** A class becomes available by
  carrying a stereotype, and the metadata is written at compile time. If a downstream repository
  registered an implementation of a surf-api interface through `@AutoService` and expected surf-api
  to find it, that no longer works — surf-api asks the container, not the service loader.
- **An interface's companion no longer *is* its implementation.** This is the one change in section
  7 that will break your code, and it is mechanical. 35 api types used to declare
  `companion object : Foo by someService`, so `Foo.doThing()` worked as a shorthand for
  `theFoo.doThing()`. Those delegations are gone; the companion now holds the instance and nothing
  else. Every such call gains one hop:

  | Was | Is |
  |---|---|
  | `SurfPaperNmsSpawnPackets.despawn(ids)` | `SurfPaperNmsSpawnPackets.INSTANCE.despawn(ids)` |
  | `SurfEventBus.callAsync(event)` | `SurfEventBus.INSTANCE.callAsync(event)` |
  | `SurfReflection.createProxy<T>()` | `SurfReflection.INSTANCE.createProxy<T>()` |
  | `SurfApiCore.getPlayer(uuid)` | `SurfApiCore.instance.getPlayer(uuid)` |
  | `Foo.Companion.bar()` *(from Java)* | `Foo.Companion.getINSTANCE().bar()` |

  The compiler finds every one of them, so the migration is "compile, follow the unresolved
  references". The member is `INSTANCE` on most types and `instance` on `SurfApiCore`,
  `SurfConfigApi` and `SuspendRequirementService`, which already had it under that name.

  It is not a cosmetic change. Interface delegation stores the delegate in a field, so the
  companion's initialiser has to resolve the service — and a Kotlin interface with default methods
  is initialised as part of initialising its own implementation, which the container does *while it
  is building that very bean*. The result was a cycle the container could not answer: it is
  part-way through creating the instance the initialiser is asking for. The service loader never
  noticed because it needed no running container. Reading the instance through a getter means the
  initialiser stores the lookup instead of performing it.

What is settled:

- A class becomes a bean by carrying a stereotype (`@Service`, `@Repository`, or your own
  annotation meta-annotated with `@BeanMeta`). Its constructor parameters are its dependencies.
- There is one **root** container per process, owned by surf-api, and one **child** container per
  application — per plugin, per microservice. Resolution walks upwards only, so one application
  cannot reach into another's beans.
- A bean crosses that boundary by declaring `Scope.SHARED`, which publishes it into the root. One
  broker connection can then serve every application in the process, which is what the existing
  `ServerRabbitMQApi` / Redis / NATS integrations need.
- Handler registration (`@RabbitHandler`, `@OnRedisEvent` and the like) is done by a
  `BeanPostProcessor` in the owning library, the same way Spring does it — not by the container
  knowing about brokers.
- Metadata is written at compile time by KSP into `META-INF/surf-api/beans/`, so the container
  can build the graph, evaluate conditions and report a cycle or an unsatisfiable parameter
  before it loads a single bean class.

One `ServiceLoader` seam remains, and only one: the container cannot resolve itself. The platform
publishes a `ContainerAccess` provider right after it starts the root, and `SurfContainer` finds
that one provider. Everything else that used to be looked up this way is a bean.

Two of surf-api's own lookups are deliberately still on the old mechanism: `SurfComponentApi` and
`ComponentService`. Those are the previous component framework, which this release removes
outright, so converting them would be work thrown away. They disappear together with
`requiredService` itself.

A separate note will follow for the broker repositories once the container is in place, with the
actual before-and-after for each.

## 9. The component framework was removed

**If your plugin has classes implementing `SurfComponent`, this is the change that will cost you
the most work.** The scanning component framework is gone, in full:

| Removed | Replaced by |
|---|---|
| `dev.slne.surf.api.shared.api.component.SurfComponent` | `dev.slne.surf.api.di.SurfComponent` |
| `…shared.api.component.types.Service` / `.Repository` | `dev.slne.surf.api.di.Service` / `.Repository` |
| `…shared.api.component.Priority` | `dev.slne.surf.api.di.Priority` |
| `…shared.api.component.SurfComponentMeta` | `dev.slne.surf.api.di.BeanMeta` |
| `…shared.api.component.condition.ComponentCondition` | `dev.slne.surf.api.di.condition.BeanCondition` |
| `…shared.api.component.processor.ComponentPostProcessor` | `dev.slne.surf.api.di.BeanPostProcessor` |
| `SurfComponentApi`, `surfComponentApi`, `AbstractComponent`, `ComponentService` | the container |
| `surfComponentApi.load(this)` / `.enable(this)` / `.disable(this)` | `SurfContainer.startChild(this)` / `.enableFor(this)` / `.stopFor(this)` |

The conditions are all in `dev.slne.surf.api.di.condition` now, and several changed name rather
than just package — the old set said `DependsOn` for two different ideas, and the new one does not:

| Removed | Replaced by |
|---|---|
| `ConditionalOn` | `ConditionalOn` (unchanged; takes a `BeanCondition`) |
| `ConditionalOnProperty`, `ConditionalOnEnvironment` | same names |
| `ConditionalOnMissingComponent` | `ConditionalOnMissingBean` — **not** the same question; see below |
| `DependsOnComponent` | `DependsOnBean` |
| `DependsOnClass` | `ConditionalOnClass` |
| `DependsOnClassName` | `ConditionalOnClassName` |
| `DependsOnPlugin` | `ConditionalOnPlugin` |
| `DependsOnOnePlugin` | `ConditionalOnAnyPlugin` |
| `ConditionalOnProductionEnvironment`, `ConditionalOnDevelopmentEnvironment` | nothing — write the environment out: `@ConditionalOnEnvironment(environments = ["production"])` |

Most of the remaining work is rewriting imports. Three differences are not renames and need a
decision:

- **A component declared its dependencies by being found; a bean declares them in its
  constructor.** That is the point of the change. A component that reached for a collaborator
  through a service lookup takes it as a constructor parameter instead, and the container orders
  the graph for you. `T?`, `List<T>`, `Set<T>` and `Lazy<T>` parameters all mean something: see
  `.claude/skills/surf-api-components/`.
- **`AbstractComponent` chained the phases**; calling `enable()` would run `load()` first if it had
  not run. The container does not chain: it runs every bean's `load`, then every bean's `enable`,
  each in dependency order. That is what makes a two-phase resource work, and it means a bean that
  relied on the chaining has to put the work in the phase it belongs to.
- **`@ConditionalOnMissingBean` is spelled like `@ConditionalOnMissingComponent` and asks a
  different question.** The old one looked at which components had *actually loaded*, in priority
  order, so it could mean "step in because that one was conditioned out".
  `@ConditionalOnMissingBean` looks at what is *declared*, the parents' beans included, and is
  evaluated before anything is built — so it can never hold for a bean declared in the same
  module. It is for "nobody has provided one of these, so here is a default", which is the case it
  is good at. A guard that really meant "that one was skipped" has to test the same thing that
  skipped it.

The plugin lifecycle now follows the server's own phases. Where you called the three
`surfComponentApi` methods, call:

```kotlin
class MyPlugin : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() { SurfContainer.startChild(this) }
    override suspend fun onEnableAsync() { SurfContainer.enableFor(this) }
    override suspend fun onDisableAsync() { SurfContainer.stopFor(this) }
}
```

`startChild` reads your plugin's own bean metadata off your own jar and builds a container below
surf-api's root. Resolution walks upwards only: you see surf-api's beans, surf-api never sees
yours. `local/paper-plugin-test` is a working example of the whole shape, conditions included.

## 10. `runServer` was removed

The `runServer` task is gone, along with the `xyz.jpenilla.run-paper` plugin that used to bring a
run task in and the `runDirectory` property on `surfMinestomServer { }`. It never worked
reliably, so there is no replacement task.

Start a server the way the manifest requires:

```
./gradlew shadowJar
java -jar build/libs/<name>-all.jar
```

`java -jar` on the shaded jar is not a preference. A Minestom server's agents are declared in the
jar manifest, and a manifest agent only applies when the JVM starts on that jar — launching the
main class off a classpath skips the mixin transformer with no error. If you want a task, write
one in your own build; it has to be an `Exec`, not a `JavaExec`, for the same reason.

## 11. Hidden binary-compatibility declarations were removed

The inventory framework carried `@Deprecated(level = DeprecationLevel.HIDDEN)` overloads and one
hidden class (`ViewContainerBackHintComponent`); `PacketOperation.kt` carried a hidden non-inline
`PacketOperation(block)`, which is why the inline one was named `PacketOperationInline` on the JVM
and is now simply `PacketOperation`. All of them were emitted purely so plugins compiled against an
older surf-api would still link. After the module renames and the descriptor changes above, no
previously compiled plugin can link against 4.0.0 regardless, so they served nobody and have
been removed.

If you compile against 4.0.0 you will not notice; a hidden declaration was never visible from
source.

## 12. The platform-neutral half of the inventory framework moved packages

Paper and Minestom each carried a full copy of the inventory framework, 61 files per platform.
21 of those files name no platform type at all, so they now live once in the new
`surf-api-inventory-api` artifact under a package of their own:

| Old package | New package |
|---|---|
| `dev.slne.surf.api.paper.inventory.framework.…` | `dev.slne.surf.api.inventory.framework.…` |
| `dev.slne.surf.api.minestom.inventory.framework.…` | `dev.slne.surf.api.inventory.framework.…` |

The subpackage and the type names are unchanged — only the prefix is, and the platform segment is
gone. These are the declarations that moved:

- `framework.dsl`: the `IFContext` / `IFConfinedContext` / `IFOpenContext` / `IFCloseContext` /
  `IFSlotClickContext` extensions (`cancel`, `openForPlayer`, `openForEveryone`), `LayoutBuilder`
  and `ViewConfigBuilder.layout`
- `framework.view`: `InventoryFrameworkDSL`
- `framework.view.container.component`: `ViewContainerComponent`, `ViewBlockCellComponent`
- `framework.view.icon`: `ViewIconColor`, `ViewIconType`
- `framework.view.pagination`: `PaginationPageIndicator`
- `framework.view.settings`: `ViewRows`, `ViewFontMetrics`, `ViewHeaderGeometry`,
  `PaginationViewRows`, `PaginationEmptyRows`, `PaginationButtonPosition`
- `framework.view.settings.align`: `TextAlignment`, `TextAlignmentOptions`
- `framework.view.util`: `GlyphShift`, `shift`

**There are no deprecated typealiases in the old packages.** An import of one of the names above
from `dev.slne.surf.api.paper.inventory` or `dev.slne.surf.api.minestom.inventory` fails to
resolve; change the import. The aggregates cover an artifact rename, not a package move, so
`surf-api-paper` and `surf-api-minestom` keep resolving and still export every one of these types
— you only edit `import` lines, never your build file.

Everything else stayed where it was, per platform, because it names a platform type: the views
themselves (`AbstractSurfView`, `AbstractPaginatedSurfView`, the DSL builders and contexts), the
buttons, `ViewIcon`, the state helpers, the container and its DSL, and the three settings types
(`SurfViewSettings`, `SimpleViewSettings`, `PaginatedViewSettings`) with their defaults and
builders. Those keep their `…paper.inventory…` and `…minestom.inventory…` packages, so an import
of `AbstractSurfView` or `surfView` does not change.

## 13. Checklist for a downstream repository

1. Replace any coordinate from table 3; the others keep working.
2. Rewrite the moved package prefixes (section 6) — `core.util` → `common.util` and the rest. This
   is the one that touches the most files, and `logger()` alone will account for most of them. No
   build file changes.
3. Remove every fastutil type and every `to*` bridge helper (section 7). Compile.
4. Check your `@ConfigSerializable` classes for fastutil-typed fields (section 7).
5. If you depend on an `-api` aggregate and resolve an implementation class at runtime, verify it
   is still on your runtime classpath (the open question in section 2).
6. Rewrite every inventory import that names one of the types in section 12; the other
   inventory imports are unchanged.
7. Compile, and add `.INSTANCE` to every call that went through an api type's companion
   (section 8). The compiler lists them all.
8. If you published an implementation of a surf-api interface with `@AutoService`, stop expecting
   surf-api to find it (section 8).
9. Rewrite every `SurfComponent` as a bean and start your own container from the plugin's three
   phases (section 9). This is the largest item on the list.
