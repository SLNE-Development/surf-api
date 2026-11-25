# Inside a version module

Four exist: `platform/paper/nms/v1-21-11`, `v26-1`, `v26-2`, `v26-3`. Each compiles ~50 Kotlin
files in the package `dev.slne.surf.api.paper.server.nms.<versionId>`, where `versionId` is
`v1_21_11`, `v26_1`, `v26_2`, `v26_3`. `v1-21-11` holds all of them itself. The 26.x modules compile
the shared sources of `platform/paper/nms/shared-26` through the `surf.nms-shared-26`
convention. The shared sources are written as `v26-3` (package segment `v26_3`, class prefix
`V26_3`, set in `shared-26/template.properties`), which compiles them as they are; `v26-1` and
`v26-2` compile copies with their own names. Each module keeps only the files that differ: about
ten each in `v26-2` and `v26-3`, about twenty in `v26-1`. In `v26-1` and `v26-2`, a module file
with the same path as a shared file replaces it; `v26-3` cannot override a shared file. Every class name is prefixed with the matching `classPrefix`
(`V1_21_11`, `V26_1`, …) and carries `@Suppress("ClassName")`, because the prefix breaks Kotlin's
naming convention.

Note the package: the *contracts* live under `dev.slne.surf.api.paper.nms`, the *implementations*
under `dev.slne.surf.api.paper.server.nms.<versionId>`. The `server` segment is the giveaway that you
are looking at a version module.

## Layout, using `v26-3`

| Path | Contents |
| --- | --- |
| `V26_3NmsConfiguration.kt` | the `@ConditionalOnMinecraftVersion(NmsVersion.V26_3)` configuration: scans the `@NmsComponent` classes and publishes `NmsPacketListeners` and the scoreboard factory through `@Bean` methods |
| `V26_3NmsPacketBridgeHandler.kt` | `@NmsComponent` implementing `NmsPacketBridgeHandler`; casts `Any` to `net.minecraft.network.protocol.Packet<*>` and delegates to the packet registry |
| `bridges/V26_3SurfPaperNms*BridgeImpl.kt` | one per contract: `Bridge`, `CommonBridge`, `EntityBridge`, `PortalBridge`, `PlayerBridge`, `ItemBridge`, `NbtBridge`, `GlowingBridge`, `StatsBridge`, `LootTableBridge`, `CommandArgumentTypesBridge` |
| `bridges/V26_3PacketListenerApiImpl.kt` | the packet listener API, the one non-`Bridge`-suffixed implementation |
| `bridges/packets/` | `V26_3PacketOperationImpl`, `V26_3SurfPaperNmsPacketBridgesImpl`, then `block/`, `entity/`, `player/` (`PlayerPackets`, `PlayerChatPackets`, `PlayerToastPackets`) |
| `extensions/nms-extensions.kt` | the `toNms()` / `toBukkit()` conversion layer — `CraftPlayer`, `CraftWorld`, `CraftItemStack`, `CraftBlockData`, `CraftMagicNumbers`, `PaperAdventure` |
| `extensions/AdventureNBT.kt` | Adventure ↔ NMS NBT conversion |
| `glow/` | `V26_3SurfGlowingApiImpl`, `V26_3GlowingLifecycleHandler`, `V26_3TeamData`, `block/BlockGlowingData`, `block/BlockPlayerData`, `entity/V26_3EntityGlowingData`, `entity/EntityPlayerData` |
| `listener/packets/` | `V26_3NmsPacketImpl`, `V26_3PacketRegistry`, then `clientbound/` and `serverbound/` wrapper impls |
| `packet/listener/` | `V26_3ChannelInjector`, `V26_3CommandSendPacketBlockerListenerImpl`, `V26_3GlowingPacketListener` |
| `packet/lore/` | `V26_3PacketLoreListener`, `V26_3PacketLoreRegistry` |
| `reflection/` | `V26_3NmsReflections`: one `@ReflectionProxy` interface per target class and the facade `object` that delegates to them |
| `region/V26_3TickThreadGuard.kt` | Folia tick-thread checks |
| `scoreboard/V26_3PlayerNmsScoreboardImpl.kt` | the per-player packet scoreboard behind the `sidebar/` api |

No version module has a `src/main/resources` directory. surf-api's root scan finds the
configuration; see the runtime selection in `SKILL.md`.

`v1-21-11`'s item bridge writes the final `Item.components` field through `unsafe`, because
reflection proxies do not write final fields.

## The configuration class

`V26_3NmsConfiguration.kt` (from `shared-26`) decides which beans the module contributes:

```kotlin
@Suppress("ClassName")
@OptIn(NmsUseWithCaution::class)
@Configuration(proxyBeanMethods = false)
@ConditionalOnMinecraftVersion(NmsVersion.V26_3)
@ComponentScan(
    useDefaultFilters = false,
    includeFilters = [ComponentScan.Filter(NmsComponent::class)],
)
class V26_3NmsConfiguration {

    @Bean
    fun nmsPacketListeners(
        loreListener: V26_3PacketLoreListener,
        glowingListener: V26_3GlowingPacketListener,
    ): NmsPacketListeners {
        return NmsPacketListeners(listOf(loreListener, glowingListener))
    }

    @Bean
    fun nmsPlayerScoreboardFactory(): PlayerNmsScoreboardFactory {
        return PlayerNmsScoreboardFactory(::V26_3PlayerNmsScoreboardImpl)
    }
}
```

Three invariants:

- A bean is an `@NmsComponent` class whose constructor Spring can call, with its collaborators as
  constructor parameters; no bean is a Kotlin `object` or reached through one.
- `@ConditionalOnMinecraftVersion` names the module's own entry; in `shared-26` that is
  `NmsVersion.V26_3`, which the copy task rewrites for the older modules.
- Startup work that needs the server's registries goes into a `SurfLifecycle.onEnable` of a bean,
  as the item bridge does for `CreativeOrderComparator.init()`, not into a field initialiser.

## The build file

Identical across the 26.x modules except the bundle string; `v1-21-11` leaves out
`surf.nms-shared-26`:

```kotlin
plugins {
    id("surf.module-impl")
    id("surf.nms-shared-26")
    id("io.papermc.paperweight.userdev") apply true
}

kotlin {
    compilerOptions {
        optIn.add("dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi")
    }
}

dependencies {
    api(projects.surfApiPaperNmsApi)
    api(projects.surfApiCoreImpl)

    paperweight.paperDevBundle("26.3.build.+")

    // CreativeOrderComparator indexes creative-mode items with two maps that have no JDK
    // counterpart: one keyed by Paper's own ItemStackLinkedSet.TYPE_AND_TAG hash strategy, and one
    // keyed by reference identity. The dev bundle happens to carry fastutil, but relying on that is
    // not a dependency declaration.
    compileOnly(libs.fastutil)

    compileOnly(libs.placeholder.api)
}

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
```

- `surf.module-impl` → `core-convention`, plus an assertion that the project name marks it as an
  implementation. `surfModuleRole` matches `-nms-v<digit>`, which is why `surf-api-paper-nms-v26-3`
  passes.
- `surf.nms-shared-26` copies the shared 26.x sources into `build/generated/nms-template/kotlin`
  with the module's package segment and class prefix, minus the files the module overrides, and
  compiles them with the module.
- `io.papermc.paperweight.userdev` is declared once in the root `build.gradle.kts` as
  `version "2.0.0-beta.24" apply false` and applied here.
- `paperweight.paperDevBundle(…)` is the only per-version line. The live shapes are
  `"1.21.11-R0.1-SNAPSHOT"` (full Maven version), `"26.1+"` (open range) and `"26.2.build.+"` /
  `"26.3.build.+"` (build-qualified range). `NmsVersion.paperDevBundle` records the string a newly
  generated module gets and matches each existing module's value.
- The generator's build-file template carries `compileOnly(libs.fastutil)` with its comment, so a
  generated `build.gradle.kts` matches the one above apart from the dev bundle string.
- The `spigot-api` exclusion is repeated in `platform/paper/impl/build.gradle.kts` too.

## Version drift to expect

Real examples from the tree, useful as `transformations` fodder when generating a new module:

- **Renamed NMS classes.** `v1-21-11` imports `net.minecraft.resources.ResourceLocation`; `v26-1`,
  `v26-2` and `v26-3` import `net.minecraft.resources.Identifier`. A `renameClass(oldFqn, newFqn)`
  transformation rewrites both the import and every in-code simple-name reference.
- **Renamed files.** The generator and the shared-source copy rewrite `classPrefix` inside paths,
  so a prefixed file name follows along automatically — an unprefixed one does not. `v26-1`'s
  override of `glow/entity/V26_1EntityGlowingData.kt` only replaces the shared file because both
  carry the prefix.
- **Reflected members.** `v26-1` has no `ServerLevel.ENTITY_COUNTER` proxy, and `v1-21-11` reflects
  only part of the chat state. The `@ReflectionProxy` interfaces are version-neutral source, but
  each module resolves them against its own Mojang-mapped classpath when the facade object loads.
- **Component and data-component churn.** `V26_3SurfPaperNmsCommonBridgeImpl` reaches into
  `net.minecraft.core.component.DataComponents`,
  `net.minecraft.world.item.component.Compostable` and
  `net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders` — the kind of
  API that moves between minors and shows up as a compile error right after generation.

## The packet registry

`listener/packets/V26_3PacketRegistry.kt` is the table that decides which raw NMS packets get an API
wrapper:

```kotlin
registerServerboundPacket(ServerboundSignUpdatePacket::class) { V26_3SignUpdatePacketImpl(it) }
registerServerboundPacket(
    ServerboundCustomPayloadPacket::class,
    V26_3ServerboundCustomPayloadPacketImpl::accepts,
) { V26_3ServerboundCustomPayloadPacketImpl(it) }
registerClientboundPacket(ClientboundDisconnectPacket::class) { V26_3ClientboundDisconnectPacketImpl(it) }
```

Two maps keyed by `Class<out Packet<*>>`, filled in an `init` block. The optional `accepts` predicate
wraps the factory so a non-matching packet yields `null` and passes through untouched.
`V26_3NmsPacketBridgeHandler` is the thin `NmsPacketBridgeHandler` adapter over it, and
`V26_3NmsPacketImpl.getFromApi(apiPacket).nmsPacket` is the unwrap side.

To expose a new packet to listeners: add the API interface under
`platform/paper/api/.../nms/listener/packets/`, add a `V<prefix>…PacketImpl` per version module, and
register it in each `V<prefix>PacketRegistry`.
