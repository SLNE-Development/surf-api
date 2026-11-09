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
| `surf-api-inventory-api` | The platform-neutral half of the inventory framework — view settings, header geometry, text alignment, the container component base and the DSL markers. Paper and Minestom each carried their own copy; see section 10. |

## 6. fastutil is gone

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

## 7. `requiredService` is being replaced by a container

The service-loader lookup (`requiredService<T>()`) is being replaced by a dependency-injection
container that works the way Spring's does: constructor injection, scopes, conditions, an
explicit lifecycle and post processors.

**This part of 4.0.0 is still being built.** It is listed here so the shape is known, not so you
can migrate against it yet — the contracts are in `surf-api-di-api` and may still move. What is
settled:

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

One `ServiceLoader` seam remains, and only one: the container cannot resolve itself.

A separate note will follow for the broker repositories once the container is in place, with the
actual before-and-after for each.

## 8. `runServer` was removed

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

## 9. Hidden binary-compatibility declarations were removed

The inventory framework carried `@Deprecated(level = DeprecationLevel.HIDDEN)` overloads and one
hidden class (`ViewContainerBackHintComponent`), emitted purely so plugins compiled against an
older surf-api would still link. After the module renames and the descriptor changes above, no
previously compiled plugin can link against 4.0.0 regardless, so they served nobody and have
been removed.

If you compile against 4.0.0 you will not notice; a hidden declaration was never visible from
source.

## 10. The platform-neutral half of the inventory framework moved packages

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

## 11. Checklist for a downstream repository

1. Replace any coordinate from table 3; the others keep working.
2. Remove every fastutil type and every `to*` bridge helper (section 6). Compile.
3. Check your `@ConfigSerializable` classes for fastutil-typed fields (section 6).
4. If you depend on an `-api` aggregate and resolve an implementation class at runtime, verify it
   is still on your runtime classpath (the open question in section 2).
5. Rewrite every inventory import that names one of the types in section 10; the other
   inventory imports are unchanged.
6. Leave `requiredService` alone for now. The container note will follow.
