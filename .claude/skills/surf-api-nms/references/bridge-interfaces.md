# The version-independent layer

Two directories hold it:

- `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/nms/` — the contracts consumers call.
- `platform/paper/nms/api/src/main/kotlin/dev/slne/surf/api/paper/nms/common/` — the plumbing the
  version modules plug into.

## The contracts in `platform/paper/api`

| File | Shape | Notes |
| --- | --- | --- |
| `nms/SurfPaperNmsBridge.kt` | companion-delegating interface | packet-listener registration; the only bridge at the `nms` package root |
| `nms/nms-opt-in.kt` | `@RequiresOptIn` annotation | `NmsUseWithCaution`, `Level.WARNING`, `AnnotationRetention.BINARY` |
| `nms/bridges/SurfPaperNmsCommonBridge.kt` | companion-delegating | entity ids, block state ids, container counter, compostables, Velocity secret, online mode, `clearDialogs`, server IP |
| `nms/bridges/SurfPaperNmsEntityBridge.kt`, `…PlayerBridge`, `…ItemBridge`, `…NbtBridge`, `…PortalBridge`, `…StatsBridge`, `…LootTableBridge`, `…GlowingBridge`, `…CommandArgumentTypesBridge` | companion-delegating | one per domain |
| `nms/bridges/packets/SurfPaperNmsPacketBridges.kt` | companion-delegating | packet-operation factory |
| `nms/bridges/packets/PacketOperation.kt` | interface + inline builder | `execute(player)`, `add`, `plus`; `PacketOperation { … }` builder |
| `nms/bridges/packets/block/SurfPaperNmsBlockPackets.kt`, `entity/SurfPaperNmsSpawnPackets.kt`, `player/SurfPaperNmsPlayerPackets.kt`, `player/SurfPaperNmsPlayerChatPackets.kt`, `player/SurfPaperNmsPlayerToastPackets.kt` | companion-delegating | packet construction per area |
| `nms/bridges/data/chat/*.kt` | plain data classes | mirrors of NMS chat types, see below |
| `nms/listener/packets/**` | interfaces | `NmsPacket`, `NmsServerboundPacket`, `NmsClientboundPacket` and the concrete wrappers (`SignUpdatePacket`, `RenameItemPacket`, `CommandSuggestionPacket`, `ServerboundCustomPayloadPacket`, `DisconnectPacket`, `SystemChatPacket`) |

### The companion-delegation pattern, in full

```kotlin
@NmsUseWithCaution
interface SurfPaperNmsCommonBridge {
    fun nextEntityId(): Int
    // …

    companion object : SurfPaperNmsCommonBridge by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsCommonBridge>()
```

Three things are load-bearing:

1. The private top-level `bridge` val, **outside** the interface. A companion object cannot hold the
   `by` target it delegates to, so the service lookup has to live at file scope.
2. `companion object : <Interface> by bridge` — this is what makes `SurfPaperNmsCommonBridge.foo()`
   resolve. Every method you add to the interface appears on the companion for free.
3. `val INSTANCE get() = bridge` — for callers that need the object itself, and for the
   `InternalNmsBridge.get()` / `InternalPacketListenerApiBridge.get()` downcasts described below.

`SurfPaperNmsBridge.kt` writes the annotation as `@NmsUseWithCaution` on the private val rather than
`@OptIn(NmsUseWithCaution::class)`; both appear in the tree and both compile.

### Mirror types

`nms/bridges/data/chat/` exists because signed chat needs NMS-shaped data on the API side.
`PlayerChatMessageMirror` is a `@Serializable` data class with nested `FilterMask`,
`SignedMessageLink` and `SignedMessageBody` types, built from `ByteArray`, `Instant`, `UUID`,
`BitSet` and Adventure's `Component`. `RemoteChatSessionData`, `PlayerChatSessionSnapshot`,
`IncomingChatChainMirror` and `LastSeenMessagesValidatorMirror` do the same for their NMS
counterparts. When a bridge method needs to hand an NMS structure across the boundary, add a mirror
here rather than widening the signature to `Any`.

`SurfPaperNmsPlayerChatPackets` shows the alternative: flatten the structure into primitives and
Adventure types in the parameter list, with
`typealias LastSeenMessagesPacked = Map<Int, SignedMessage.Signature?>`.

## The plumbing in `platform/paper/nms/api`

| File | What it is |
| --- | --- |
| `NmsProvider.kt` | the factory interface + `companion object { val current: NmsProvider by lazy { … } }` |
| `NmsVersionResolver.kt` | `object` with `val current: NmsVersion by lazy { … }` from `Bukkit.getMinecraftVersion()` |
| `InternalNmsBridge.kt` | `interface InternalNmsBridge : SurfPaperNmsBridge` — adds `handleServerboundPacket`/`handleClientboundPacket`; `get()` downcasts `SurfPaperNmsBridge.INSTANCE` |
| `InternalPacketListenerApiBridge.kt` | same shape over `SurfPaperPacketListenerApi` |
| `NmsPacketBridgeHandler.kt` | `wrapServerboundPacket(Any)`, `wrapClientboundPacket(Any)`, `unwrapPacket(NmsPacket)` — raw NMS crosses as `Any` |
| `AbstractChannelInjector.kt` | abstract Netty injector + nested `AbstractPacketHandler : ChannelDuplexHandler` |
| `CommandSendPacketBlockerListener.kt` | abstract `PacketListener` holding `blockedPlayers` and `receivedFirstCommandPacket` |
| `PacketLoreRegistry.kt` | `register`/`unregister` for `SurfPaperPacketLoreHandler` |
| `GlowingLifecycleHandler.kt` | `removeAllGlowingOnQuit`, `getBlockGlowSpawnOperationForChunk` |
| `scoreboard/PlayerNmsScoreboard.kt` | packet-based sidebar contract |
| `dummy/DummyEntityEquipment.kt` | placeholder equipment |

### `NmsProvider`'s factory list

Every member a version module must supply:

```
version: NmsVersion                 plugin: JavaPlugin
createNmsBridge()                   createCommonBridge()
createEntityBridge()                createPortalBridge()
createPlayerBridge()                createItemBridge()
createNbtBridge()                   createGlowingBridge()
createStatsBridge()                 createLootTableBridge()
createCommandArgumentTypesBridge()  createPacketBridges()
createBlockPackets()                createSpawnPackets()
createPlayerPackets()               createPlayerChatPackets()
createPlayerToastPackets()          createTickThreadGuard()
getLibLoaderBridge()                getPacketBridgeHandler()
createPacketLoreRegistry()          createGlowingLifecycleHandler()
createGlowingApi()                  createPlayerScoreboard(player)
createChannelInjector()             createPacketListenerApi()
createCommandSendPacketBlockerListener(blockedPlayers)
createPacketListeners()             initialize()               shutdown()
```

No member has a default body, so adding one breaks all four version modules at compile time — which
is the point.

### How `NmsProvider.current` works

`NmsProvider.kt`, companion object:

1. `val version = NmsVersionResolver.current`.
2. `javaClass.classLoader.getResources("META-INF/surf-api/nms-providers")` — the constant is
   `NmsProviderConfig.NMS_PROVIDERS_DIRECTORY` in `foundation/internal/.../nms/NmsProviderConfig.kt`.
3. For each URL: if `url.protocol == "jar"`, open the jar and read every entry under that directory
   ending in `.json`; otherwise treat it as a directory and read its `.json` files. Each file decodes
   to `List<NmsProviderMeta>` (`version: NmsVersion`, `implementation: String`). A parse failure is
   logged at severe and skipped, not rethrown.
4. Log `Discovered %s NmsProviders`, then `metas.find { it.version == version }`, else warn and fall
   back to `metas.maxByOrNull { it.version.versionPrefix }`, else
   `error("No NmsProvider implementations found")`.
5. `Class.forName(selectedMeta.implementation)`, require a single-argument `(JavaPlugin)` constructor
   — the error message says so explicitly — and `newInstance(JavaPlugin.getProvidingPlugin(NmsProvider::class.java))`.

The whole block is wrapped in a `try`/`catch` that logs and rethrows as
`IllegalStateException("Failed to load NmsProvider", e)`. Because it is a `by lazy`, this all happens
on the first `NmsProvider.current` touch.

### Who writes the JSON

`tooling/gradle-plugin/processor/src/main/kotlin/dev/slne/surf/api/processor/nms/NmsProvidersCollector.kt`,
a KSP `SymbolProcessor` registered through
`NmsProvidersCollectorProvider` and `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.
It finds classes annotated with `NmsProviderMarker` (`ClassNames.NMS_PROVIDER`), reads the `version`
argument as an enum entry, maps each to `NmsProviderMeta(version, binaryName)`, groups by
`resolver.getModuleName().asString().split(":").last()` and in `finish()` writes
`META-INF/surf-api/nms-providers/<module>.json`.

`core-convention` (in `buildSrc`) adds `ksp(project(":surf-api-processor"))` to every module except
`surf-api-annotations` and `surf-api-internal`, so a version module gets this for free.
`@NmsProviderMarker` itself is in `foundation/internal/.../nms/NmsProviderMarker.kt` with
`AnnotationRetention.RUNTIME`.

### The proxies in `platform/paper/impl`

`platform/paper/impl/src/main/kotlin/dev/slne/surf/api/paper/server/proxy/` holds twenty files, one
per bridge, all the same three lines:

```kotlin
@NmsUseWithCaution
@AutoService(SurfPaperNmsCommonBridge::class)
class SurfPaperNmsCommonBridgeProxy :
    SurfPaperNmsCommonBridge by NmsProvider.current.createCommonBridge() {
    init { checkInstantiationByServiceLoader() }
}
```

`@AutoService` writes the `META-INF/services` entry the `requiredService` lookup needs;
`checkInstantiationByServiceLoader()` (in `common/core/api/.../util/util.kt`) asserts the caller class
starts with `java.util.ServiceLoader`. **A new bridge needs a new proxy here** — the version module
alone is not reachable.

### The packet path

`AbstractChannelInjector` is a Bukkit `Listener`; `companion object { val instance by lazy { NmsProvider.current.createChannelInjector() } }`.
`register()` calls the abstract `registerChannelInitializeListener(CHANNEL_KEY) { getOrInjectPacketHandler(it) }`
and registers the listener with `NmsProvider.current.plugin`. `getOrInjectPacketHandler` stores the
handler in a Netty `AttributeKey` with `compareAndSet`, then adds it to the pipeline with
`addBefore(getNmsChannelHandlerName(), "surf_api_packet_listener", handler)`, hopping onto the
channel's event loop when not already on it.

`AbstractPacketHandler` overrides `write` (server → client) and `channelRead` (client → server),
both funnelling into a private inline `handlePacket` that:

1. returns early via `passthrough` when `isPacket(msg)` is false,
2. returns early when `getPlayerFromConnection()` is the `NO_CONNECTION_SENTINEL`,
3. runs the NMS-level handler (`packetListenerApi::handleClientboundPacket` /
   `handleServerboundPacket`), a `null` meaning cancelled,
4. runs the bridge-level handler, which wraps the raw packet with
   `packetBridgeHandler.wrap…Packet`, passes the API wrapper through
   `InternalNmsBridge.handle…Packet`, and unwraps the result; an unwrappable packet is passed through
   unchanged, a `null` result cancels,
5. rethrows `OutOfMemoryError`, logs any other `Throwable` and passes the packet through.

The abstract hooks a version module must implement are `registerChannelInitializeListener`,
`unregisterChannelInitializeListener`, `createPacketHandler`, `getNmsChannelHandlerName`, and on the
handler `isPacket`, `getPlayerFromConnection`, `getBukkitPlayer`.

## Adding a bridge

1. Write the interface in `platform/paper/api/.../nms/bridges/`, `@NmsUseWithCaution`, Bukkit and
   Adventure types only, with the companion-delegation block and the private `requiredService` val.
2. Add a `createXBridge(): SurfPaperNmsXBridge` factory to `NmsProvider`.
3. Add an `@AutoService`-annotated proxy in `platform/paper/impl/.../proxy/`.
4. Implement `V<prefix>SurfPaperNmsXBridgeImpl` in **every** version module and return it from each
   `V<prefix>NmsProvider`.
5. If it needs reflective access, add the member to that module's `V<prefix>NmsReflections`
   interface rather than hand-rolling `Field.setAccessible`.
