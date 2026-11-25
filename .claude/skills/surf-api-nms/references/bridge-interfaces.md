# The version-independent layer

Two directories hold it:

- `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/nms/` — the contracts consumers call.
- `platform/paper/nms/api/src/main/kotlin/dev/slne/surf/api/paper/nms/common/` — the plumbing the
  version modules plug into.

## The contracts in `platform/paper/api`

The "Shape" column names the static accessor each bridge interface has, if any:

| File | Shape | Notes |
| --- | --- | --- |
| `nms/SurfPaperNmsBridge.kt` | interface, public `@InternalSurfApi` `instance` | packet-listener registration; the only bridge at the `nms` package root |
| `nms/nms-opt-in.kt` | `@RequiresOptIn` annotation | `NmsUseWithCaution`, `Level.WARNING`, `AnnotationRetention.BINARY` |
| `nms/bridges/SurfPaperNmsCommonBridge.kt` | interface, public `@InternalSurfApi` `instance` | entity ids, block state ids, container counter, compostables, Velocity secret, online mode, `clearDialogs`, server IP |
| `nms/bridges/SurfPaperNmsCommandArgumentTypesBridge.kt` | interface, `internal` `instance` | compound-tag and signed-message argument types, read by `AdventureCompoundBinaryTagArgument` and `AsyncSignedMessageArgument` |
| `nms/bridges/SurfPaperNmsEntityBridge.kt`, `…PlayerBridge`, `…ItemBridge`, `…NbtBridge`, `…PortalBridge`, `…StatsBridge`, `…LootTableBridge`, `…GlowingBridge` | interface, no companion | one per domain; injected only |
| `nms/bridges/packets/SurfPaperNmsPacketBridges.kt` | interface, `internal` `instance` | packet-operation factory, read by `PacketOperation.start()` |
| `nms/bridges/packets/PacketOperation.kt` | interface + inline builder | `execute(player)`, `add`, `plus`; `PacketOperation { … }` builder |
| `nms/bridges/packets/entity/SurfPaperNmsSpawnPackets.kt` | interface, public `@InternalSurfApi` `instance` | spawn packets |
| `nms/bridges/packets/player/SurfPaperNmsPlayerToastPackets.kt` | interface, `internal` `instance` | toast packets, read by `Toast.createOperation()` |
| `nms/bridges/packets/block/SurfPaperNmsBlockPackets.kt`, `player/SurfPaperNmsPlayerPackets.kt`, `player/SurfPaperNmsPlayerChatPackets.kt` | interface, no companion | packet construction per area; injected only |
| `nms/bridges/data/chat/*.kt` | plain data classes | mirrors of NMS chat types, see below |
| `nms/listener/packets/**` | interfaces | `NmsPacket`, `NmsServerboundPacket`, `NmsClientboundPacket` and the concrete wrappers (`SignUpdatePacket`, `RenameItemPacket`, `CommandSuggestionPacket`, `ServerboundCustomPayloadPacket`, `DisconnectPacket`, `SystemChatPacket`) |

### The `instance` pattern, in full

A bridge needs a companion with a `by surfBean` val only when surf-api's own static code reads it
and that code is not itself a bean. A bridge that is only injected is the interface plus its proxy
bean and nothing else.

```kotlin
@NmsUseWithCaution
interface SurfPaperNmsCommonBridge {
    fun nextEntityId(): Int
    // …

    companion object {
        @InternalSurfApi
        val instance by surfBean<SurfPaperNmsCommonBridge>()
    }
}
```

Three things are load-bearing:

1. `val instance by surfBean<T>()` (`foundation/spring/api/.../spring/SurfContexts.kt`). The
   delegate reads the bean from surf-api's installed root context (`SurfContexts.root`) the first
   time the property is read and again after the platform installs another root, so the lookup
   happens when the bridge is used rather than when its class is loaded.
2. The companion does **not** implement the interface, and `instance` is delegated, never
   initialised. `companion object : <Interface> by <bean>` would read the bean inside the
   companion's initialiser, i.e. during class initialisation, which can land before the root
   context is installed or while it is still creating that very bean.
3. `instance` is hidden from plugins. When other surf-api modules read it, it is public and carries
   `@InternalSurfApi`, an error-level opt-in that the root build grants surf-api's own modules:
   they write `SurfPaperNmsCommonBridge.instance.nextEntityId()`. When only `platform/paper/api`
   itself reads it, it is `internal` instead, as on `SurfPaperNmsPacketBridges`,
   `SurfPaperNmsPlayerToastPackets` and `SurfPaperNmsCommandArgumentTypesBridge`. A plugin injects
   the bridge interface into a bean, as does any surf-api class that is itself a bean.

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
| `ConditionalOnMinecraftVersion.kt` | the condition on each version module's configuration, and `OnMinecraftVersionCondition`, which compares it with the `surfNmsVersion` bean |
| `NmsComponent.kt` | the marker the version configurations scan for; no Spring stereotype, so the root scan skips it |
| `NmsVersionResolver.kt` | `resolve(minecraftVersion)` through `NmsVersion.select`, and `BEAN_NAME` of the selected-version bean |
| `NmsPacketListeners.kt` | the version module's packet listeners that `PacketApiLoader` registers |
| `InternalNmsBridge.kt` | `interface InternalNmsBridge : SurfPaperNmsBridge` — adds `handleServerboundPacket`/`handleClientboundPacket`; the channel injector receives the bridge bean under this type |
| `InternalPacketListenerApiBridge.kt` | same shape over `SurfPaperPacketListenerApi` |
| `NmsPacketBridgeHandler.kt` | `wrapServerboundPacket(Any)`, `wrapClientboundPacket(Any)`, `unwrapPacket(NmsPacket)` — raw NMS crosses as `Any` |
| `AbstractChannelInjector.kt` | abstract Netty injector + nested `AbstractPacketHandler : ChannelDuplexHandler` |
| `CommandSendPacketBlockerListener.kt` | abstract `PacketListener` that owns `blockedPlayers`, which `SuspendRequirementServiceImpl` fills, and `receivedFirstCommandPacket` |
| `PacketLoreRegistry.kt` | `register`/`unregister` for `SurfPaperPacketLoreHandler` |
| `GlowingLifecycleHandler.kt` | `removeAllGlowingOnQuit`, `getBlockGlowSpawnOperationForChunk` |
| `scoreboard/PlayerNmsScoreboard.kt` | per-player packet scoreboard that backs the `sidebar/` api |
| `scoreboard/PlayerNmsScoreboardFactory.kt` | creates a `PlayerNmsScoreboard` per player |
| `dummy/DummyEntityEquipment.kt` | placeholder equipment |

### The beans of a version module

`V<prefix>NmsConfiguration` (in `shared-26` for 26.x, in `v1-21-11` for 1.21.11) is
`@Configuration(proxyBeanMethods = false)`, `@ConditionalOnMinecraftVersion(NmsVersion.V<prefix>)`
and `@ComponentScan(useDefaultFilters = false, includeFilters = [Filter(NmsComponent::class)])`.

Scanned `@NmsComponent` classes: `V<prefix>SurfPaperNmsBridgeImpl` (`InternalNmsBridge`), the
common, entity, portal, player, item, NBT, stats, loot table and command argument type bridges,
the packet bridges and the block, spawn, player, player chat and player toast packets,
`V<prefix>PacketListenerApiImpl` (`InternalPacketListenerApiBridge`), `V<prefix>TickThreadGuard`,
`V<prefix>PacketLoreRegistry`, `V<prefix>GlowingLifecycleHandler`,
`V<prefix>CommandSendPacketBlockerListenerImpl`, `V<prefix>NmsPacketBridgeHandler`,
`V<prefix>SurfPaperNmsGlowingBridgeImpl`, `V<prefix>SurfGlowingApiImpl` (which injects surf-api's
own `Plugin`, a fallback bean of the root context), `V<prefix>ChannelInjector`,
`V<prefix>PacketLoreListener` and `V<prefix>GlowingPacketListener`.

`@Bean` methods: `nmsPacketListeners` (the injected lore and glowing listener) and
`nmsPlayerScoreboardFactory`.

Every bridge interface has exactly one bean, so consumers inject it by type. A bean missing from
the selected module fails the root context's refresh with `NoSuchBeanDefinitionException`.

### The packet path

`AbstractChannelInjector` is a Bukkit `Listener`; `companion object { val instance by surfBean<AbstractChannelInjector<*>>() }`.
`register(plugin)` calls the abstract `registerChannelInitializeListener(CHANNEL_KEY) { getOrInjectPacketHandler(it) }`
and registers the listener for `plugin`. `getOrInjectPacketHandler` stores the
handler in a Netty `AttributeKey` with `compareAndSet`, then adds it to the pipeline with
`addBefore(getNmsChannelHandlerName(), "surf_api_packet_listener", handler)`, hopping onto the
channel's event loop when not already on it.

`AbstractPacketHandler` takes the `NmsPacketBridgeHandler`, the `InternalNmsBridge` and the
`InternalPacketListenerApiBridge`, which the version's channel injector receives by constructor
injection and hands to every handler it creates. It overrides `write`
(server → client) and `channelRead` (client → server),
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
   Adventure types only. Add the `companion object { … val instance by surfBean<T>() }` block
   only when surf-api's own static code reads the bridge; otherwise injecting the interface is
   enough.
2. Implement `V<prefix>SurfPaperNmsXBridgeImpl` as an `@NmsComponent` class with a constructor
   Spring can call: once in `shared-26` (plus an override in any 26.x module that differs) and once
   in `v1-21-11`.
3. A Kotlin `object` implementation is published through a `@Bean` method of
   `V<prefix>NmsConfiguration` instead.
4. If it needs reflective access, add the member to the `@ReflectionProxy` interface of its
   target class in that module's `V<prefix>NmsReflections.kt` rather than hand-rolling
   `Field.setAccessible`.
