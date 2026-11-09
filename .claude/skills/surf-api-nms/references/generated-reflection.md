# Reflective access into NMS

Two mechanisms exist in the tree. Use the first; the second is legacy and survives only in
`v1-21-11`.

## 1. `@GenerateReflection` (current)

Declared in `foundation/annotations/src/main/kotlin/dev/slne/surf/api/shared/api/reflection/`:
`GeneratedReflection.kt` holds the annotations, `GeneratedReflectionAccessor.kt` the runtime loader.

A KSP processor generates a **Java** implementation class per annotated interface. The generated
class resolves every reflected member once in a static initializer: methods and constructors become
`MethodHandle` fields, fields and VarHandle operations become `VarHandle` (or derived `MethodHandle`)
fields. Nothing is generated at runtime — if the KSP task did not run, the lookup throws.

### The shape

```kotlin
@GenerateReflection
interface V26_3NmsReflections {
    @ReflectedField(name = "ENTITY_COUNTER", target = ServerLevel::class, isStatic = true)
    fun getEntityCounter(): AtomicInteger

    companion object : V26_3NmsReflections by generatedReflectionAccessor()
}
```

Callers then write `V26_3NmsReflections.getEntityCounter()`. `V26_3SurfPaperNmsCommonBridgeImpl` does
exactly that: `return V26_3NmsReflections.getEntityCounter().incrementAndGet()`.

`@GenerateReflection` members:

| Property | Meaning |
| --- | --- |
| `target: KClass<*>` | default compile-time target for every member. Prefer it when the class is on the compile classpath. |
| `targetName: String` | binary name of the default target, resolved by name at runtime. |
| `packageName` | package of the generated class; blank → the interface's package. |
| `className` | simple name of the generated class; blank → `<InterfaceSimpleName>Impl`. |
| `singleton` | default `true`: the generated class exposes a static `INSTANCE` and a private constructor. Keep it enabled — the handles are immutable after init. |

`generatedReflectionAccessor()` (reified, plus `KClass` and `Class` overloads) resolves
`<packageName>.<className>`, defaulting to `<InterfacePackage>.<InterfaceSimpleName>Impl`, through
`Class.forName(name, true, type.classLoader)`, then reads the static `INSTANCE` field or falls back
to a no-args constructor. Results are cached in a `ClassValue`. The failure message is explicit:
*"Generated reflection implementation '…' was not found for '…'. Run the KSP task for the module that
declares this interface."*

### Target resolution order

For every member, in order:

1. the member annotation's `target` / `targetName`,
2. `@GenerateReflection`'s `target` / `targetName`,
3. for instance members, the **first function parameter**,
4. for constructors, the **return type**.

Static members have no receiver, so they need (1) or (2). This is why
`getEntityCounter()` must spell out `target = ServerLevel::class`.

### The member annotations

**`@ReflectedField(name, isStatic, access, target, targetName, type, typeName, typeDescriptor)`** —
a `VarHandle` `get` or `set`. `access` is `ReflectedFieldAccess.GET` (default) or `SET`; final fields
are intentionally unsupported. A blank `name` falls back to the function name. The field type is
inferred from the return type (GET) or the value parameter (SET) unless `type`/`typeName`/
`typeDescriptor` is given — needed when the proxy type is generic or erased, e.g. exposing a raw
`ResourceKey` field as `ResourceKey<ChatType>` while looking it up with `type = ResourceKey::class`.

```kotlin
@ReflectedField("chatMessageChain")
fun getChatMessageChain(instance: ServerGamePacketListenerImpl): FutureChain

@ReflectedField("nextChatIndex", access = ReflectedFieldAccess.SET)
fun setNextChatIndex(instance: ServerGamePacketListenerImpl, index: Int)

@ReflectedField(name = "PAPER_RAW", isStatic = true, target = ChatProcessor::class)
fun getPaperRawChatTypeKey(): ResourceKey<ChatType>
```

**`@ReflectedMethod(name, isStatic, target, targetName, descriptor, invocation)`** — a private,
package-private or protected method. The first parameter is the receiver for instance methods; set
`isStatic = true` and drop it otherwise. A blank `descriptor` is inferred from the signature; give one
for overloads or for a `targetName`-only class.

```kotlin
@ReflectedMethod("mask")
fun getFilterMask(instance: FilterMask): BitSet
```

**`@ReflectedConstructor(target, targetName, descriptor, invocation)`** — no receiver; the parameters
are the constructor arguments and the return type is the constructed type. A `descriptor` here must be
a constructor descriptor returning `V`.

```kotlin
@ReflectedConstructor
fun createFilterMask(bitSet: BitSet): FilterMask
```

**`@ReflectedVarHandle(name, isStatic, mode, target, targetName, type, typeName, typeDescriptor, invocation)`**
— anything a plain get/set cannot do: volatile reads and writes, acquire/release, compare-and-set,
compare-and-exchange, atomic get-and-set, get-and-add, bitwise atomics. `mode` is a
`java.lang.invoke.VarHandle.AccessMode`.

```kotlin
@ReflectedVarHandle(name = "nextChatIndex", mode = VarHandle.AccessMode.GET_AND_ADD)
@ConstantIntArgument(1)
fun getAndIncreaseNextChatIndex(instance: ServerGamePacketListenerImpl): Int
// generates: return (int) nextChatIndex.getAndAdd(instance, 1);

@ReflectedVarHandle(name = "chatVisibility", mode = VarHandle.AccessMode.GET_AND_SET)
fun getAndSetChatVisibility(player: ServerPlayer, visibility: ChatVisiblity): ChatVisiblity
```

**Constant arguments** — `@ConstantIntArgument`, `@ConstantLongArgument`, `@ConstantBooleanArgument`,
`@ConstantStringArgument`, all `@Repeatable`. They are appended **after** all proxy parameters, in
annotation order, and let the public signature hide a fixed argument.

### Invocation modes

`ReflectionInvocationMode`, on `@ReflectedMethod`, `@ReflectedConstructor` and `@ReflectedVarHandle`:

| Mode | Emits | When |
| --- | --- | --- |
| `EXACT` (default) | `invokeExact(…)`, or the direct VarHandle access-mode call | all types known at compile time and the signature matches after erasure |
| `AS_TYPE_EXACT` | one `asType(…)` in the static initializer, then `invokeExact(…)` | the member is resolved by `targetName`/`descriptor` but the proxy signature is still strongly typed |
| `INVOKE` | plain `invoke(…)` | fallback for intentionally dynamic cases; conversions happen at the callsite |

For `@ReflectedVarHandle`, `EXACT` emits the access-mode method directly; the other two convert the
access mode to a `MethodHandle` with `toMethodHandle(…)` first.

`UnspecifiedReflectionTarget` is the sentinel default for every `KClass` property — Kotlin annotations
cannot default a `KClass` to `null`. Never reference it from your own code.

### Where it is used

Each version module has one `reflection/V<prefix>NmsReflections.kt`. `v26-3`'s covers
`ServerLevel.ENTITY_COUNTER`, the whole signed-chat chain on `ServerGamePacketListenerImpl`
(`chatMessageChain`, `nextChatIndex`, `messageSignatureCache`, `lastSeenMessages`,
`signedMessageDecoder`, `chatSession`, `hasLoggedExpiry`, `resetPlayerChatState`),
`MessageSignatureCache`, `LastSeenMessagesValidator`, `SignedMessageChain`, `FilterMask`,
`Entity.FLAG_GLOWING` / `FLAG_INVISIBLE` / `DATA_SHARED_FLAGS_ID`,
`ServerConnectionListener.channels`, `ServerStatsCounter.toJson` / `GSON`,
`ChatProcessor.PAPER_RAW` and `ServerPlayer.chatVisibility`.

That file is the single place to add a new reflective access for a version. Because the interface is
copied by the module generator, a member you add to the reference version propagates to the next
generated module — and breaks its compile immediately if the field moved, which is the behaviour you
want.

## 2. `reflection-remapper` (legacy, `v1-21-11` only)

`xyz.jpenilla:reflection-remapper` is pinned at `0.1.3` in `gradle/libs.versions.toml` and declared
`compileOnly(libs.reflection.remapper)` in every version module; `platform/paper/impl` carries the
`implementation` dependency.

```kotlin
@Suppress("ClassName")
@Proxies(Item::class)
interface V1_21_11ItemProxy {
    @FieldSetter("components")
    fun setComponents(item: Item, components: DataComponentMap)
}
```

built in `V1_21_11Reflection.initialize()`:

```kotlin
val remapper = ReflectionRemapper.forReobfMappingsInPaperJar()
val proxyFactory = ReflectionProxyFactory.create(remapper, V1_21_11Reflection::class.java.classLoader)
ITEM_PROXY = proxyFactory.reflectionProxy<V1_21_11ItemProxy>()
System.gc() // gc the remapper
```

`V26_1Reflection`, `V26_2Reflection` and `V26_3Reflection` keep the same `object` with the same
`initialize()` entry point, but the body is commented out — they exist only so
`V<prefix>NmsProvider.initialize()` has something to call. Do not revive it for new work; add the
member to `V<prefix>NmsReflections` instead.
