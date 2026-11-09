# Inside a version module

Four exist today: `platform/paper/nms/v1-21-11`, `v26-1`, `v26-2`, `v26-3`. Each carries ~50 Kotlin
files under `src/main/kotlin/dev/slne/surf/api/paper/server/nms/<versionId>/`, where `versionId` is
`v1_21_11`, `v26_1`, `v26_2`, `v26_3`. Every class name is prefixed with the matching `classPrefix`
(`V1_21_11`, `V26_1`, …) and carries `@Suppress("ClassName")`, because the prefix breaks Kotlin's
naming convention.

Note the package: the *contracts* live under `dev.slne.surf.api.paper.nms`, the *implementations*
under `dev.slne.surf.api.paper.server.nms.<versionId>`. The `server` segment is the giveaway that you
are looking at a version module.

## Layout, using `v26-3`

| Path | Contents |
| --- | --- |
| `V26_3NmsProvider.kt` | the `@NmsProviderMarker(NmsVersion.V26_3)` class; one line per factory |
| `V26_3NmsPacketBridgeHandler.kt` | `object` implementing `NmsPacketBridgeHandler`; casts `Any` to `net.minecraft.network.protocol.Packet<*>` and delegates to the packet registry |
| `bridges/V26_3SurfPaperNms*BridgeImpl.kt` | one per contract: `Bridge`, `CommonBridge`, `EntityBridge`, `PortalBridge`, `PlayerBridge`, `ItemBridge`, `NbtBridge`, `GlowingBridge`, `StatsBridge`, `LootTableBridge`, `CommandArgumentTypesBridge` |
| `bridges/V26_3LibLoaderBridgeImpl.kt`, `V26_3PacketListenerApiImpl.kt` | the two non-`Bridge`-suffixed ones |
| `bridges/packets/` | `V26_3PacketOperationImpl`, `V26_3SurfPaperNmsPacketBridgesImpl`, then `block/`, `entity/`, `player/` (`PlayerPackets`, `PlayerChatPackets`, `PlayerToastPackets`) |
| `extensions/nms-extensions.kt` | the `toNms()` / `toBukkit()` conversion layer — `CraftPlayer`, `CraftWorld`, `CraftItemStack`, `CraftBlockData`, `CraftMagicNumbers`, `PaperAdventure` |
| `extensions/AdventureNBT.kt` | Adventure ↔ NMS NBT conversion |
| `glow/` | `V26_3SurfGlowingApiImpl`, `V26_3GlowingLifecycleHandler`, `V26_3TeamData`, `block/BlockGlowingData`, `block/BlockPlayerData`, `entity/V26_3EntityGlowingData`, `entity/EntityPlayerData` |
| `listener/packets/` | `V26_3NmsPacketImpl`, `V26_3PacketRegistry`, then `clientbound/` and `serverbound/` wrapper impls |
| `packet/listener/` | `V26_3ChannelInjector`, `V26_3CommandSendPacketBlockerListenerImpl`, `V26_3GlowingPacketListener` |
| `packet/lore/` | `V26_3PacketLoreListener`, `V26_3PacketLoreRegistry` |
| `reflection/` | `V26_3NmsReflections` (the `@GenerateReflection` interface), `V26_3Reflection` (the `initialize()` hook) |
| `region/V26_3TickThreadGuard.kt` | Folia tick-thread checks |
| `scoreboard/V26_3PlayerNmsScoreboardImpl.kt` | packet sidebar |

`v26-2` and `v26-3` additionally have
`src/main/resources/META-INF/services/dev.slne.surf.api.paper.nms.common.NmsProvider`, written by the
generator. It is unused — see the traps in `SKILL.md`. `v26-1` and `v1-21-11` have no
`src/main/resources` directory at all.

`v1-21-11` has one extra file, `reflection/V1_21_11ItemProxy.kt`, the last live
`reflection-remapper` proxy in the tree.

## The provider class

`V26_3NmsProvider.kt` is the only file you must get exactly right; everything else it merely points
at.

```kotlin
@Suppress("ClassName")
@OptIn(NmsUseWithCaution::class)
@NmsProviderMarker(NmsVersion.V26_3)
class V26_3NmsProvider(override val plugin: JavaPlugin) : NmsProvider {
    override val version: NmsVersion = NmsVersion.V26_3

    override fun createCommonBridge(): SurfPaperNmsCommonBridge = V26_3SurfPaperNmsCommonBridgeImpl()
    override fun createGlowingBridge(): SurfPaperNmsGlowingBridge = V26_3SurfPaperNmsGlowingBridgeImpl
    override fun getLibLoaderBridge(): LibLoaderBridge = V26_3LibLoaderBridgeImpl
    override fun getPacketBridgeHandler(): NmsPacketBridgeHandler = V26_3NmsPacketBridgeHandler
    // …

    override fun createPacketListeners(): List<PacketListener> = listOf(
        V26_3PacketLoreListener,
        V26_3GlowingPacketListener,
    )

    override fun initialize() {
        V26_3Reflection.initialize()
        V26_3SurfPaperNmsItemBridgeImpl.CreativeOrderComparator.init()
    }

    override fun shutdown() { /* No cleanup needed */ }
}
```

Four invariants:

- The `(JavaPlugin)` constructor is mandatory — `NmsProvider.current` looks it up by exactly that
  signature and its failure message says so.
- `@NmsProviderMarker(NmsVersion.V26_3)` and `override val version` must agree. The marker drives
  discovery; `version` is what gets logged and compared.
- Some factories return a singleton `object` (no parentheses), others a fresh instance. Follow the
  reference module rather than guessing; `createGlowingBridge`, `getLibLoaderBridge`,
  `getPacketBridgeHandler`, `createGlowingApi` and `createChannelInjector` return objects in `v26-3`.
- `initialize()` runs from `PacketApiLoader.onEnable()`, after `packetEvents.init()` and before any
  packet listener is registered. Anything that resolves reflection handles or builds static caches
  belongs here, not in a field initialiser.

## The build file

Identical across all four modules except the bundle string:

```kotlin
plugins {
    id("surf.module-impl")
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

    compileOnly(libs.placeholder.api)
    compileOnly(libs.reflection.remapper)
    compileOnly(libs.mccoroutine.folia.api)
    compileOnly(libs.scoreboard.library.api)

    implementation(libs.bytebuddy)
}

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
```

- `surf.module-impl` → `core-convention`, plus an assertion that the project name marks it as an
  implementation. `surfModuleRole` matches `-nms-v<digit>`, which is why `surf-api-paper-nms-v26-3`
  passes.
- `io.papermc.paperweight.userdev` is declared once in the root `build.gradle.kts` as
  `version "2.0.0-beta.24" apply false` and applied here.
- `paperweight.paperDevBundle(…)` is the only per-version line. The three live shapes are
  `"1.21.11-R0.1-SNAPSHOT"` (full Maven version), `"26.1+"` / `"26.2+"` (open range) and
  `"26.3.build.+"` (build-qualified range). `NmsVersion.paperDevBundle` in the generator records the
  canonical string for each version.
- `libs.reflection.remapper` stays `compileOnly` even though only `v1-21-11` still uses it at
  runtime; `platform/paper/impl` carries the `implementation` dependency.
- The `spigot-api` exclusion is repeated in `platform/paper/impl/build.gradle.kts` too.

## Version drift to expect

Real examples from the tree, useful as `transformations` fodder when generating a new module:

- **Renamed NMS classes.** `v1-21-11` imports `net.minecraft.resources.ResourceLocation`; `v26-1`,
  `v26-2` and `v26-3` import `net.minecraft.resources.Identifier`. A `renameClass(oldFqn, newFqn)`
  transformation rewrites both the import and every in-code simple-name reference.
- **Renamed files.** `v26-1` has `glow/entity/EntityGlowingData.kt`; `v26-2` and `v26-3` have
  `V26_2EntityGlowingData.kt` / `V26_3EntityGlowingData.kt`. The generator rewrites `classPrefix`
  inside paths, so a prefixed file name follows along automatically — an unprefixed one does not, and
  needs a manual rename (or a `transformFile`).
- **Dropped reflection.** `v1-21-11`'s `V1_21_11Reflection.initialize()` builds a
  `ReflectionRemapper.forReobfMappingsInPaperJar()` and a `ReflectionProxyFactory`; the three 26.x
  modules have that body commented out and keep only the empty `initialize()`. The replacement is
  `@GenerateReflection`, which is version-neutral source but resolves against each module's own
  Mojang-mapped classpath.
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
