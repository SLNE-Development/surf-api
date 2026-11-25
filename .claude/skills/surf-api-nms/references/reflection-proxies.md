# Reflection proxies

Source: `common/reflection/api/src/main/kotlin/dev/slne/surf/api/reflection/` -
`reflection-annotations.kt` (the annotations), `ReflectionProxies.kt` (`ReflectionProxies.get`,
`reflectionProxy<T>()`, `ReflectionProxyException`), `ReflectionProxyFactory.kt`,
`ProxyMemberBinder.kt` and `ProxyLookups.kt`; the field handles in `field-handles.kt`,
`FieldHandleKind.kt` and `FieldHandleClasses.kt`. The plugin-context registration is
`ReflectionProxyContributor` in `common/core/impl/.../reflection/`.

## Declaring a proxy

```kotlin
@ReflectionProxy(ServerGamePacketListenerImpl::class)   // or targetName = "binary.Name"
interface V26_3PacketListenerReflection {

    @ReflectedField("chatSession")                       // GET is the default access
    fun getRemoteChatSession(instance: ServerGamePacketListenerImpl): RemoteChatSession?

    @ReflectedField("chatSession", FieldAccess.SET)
    fun setRemoteChatSession(instance: ServerGamePacketListenerImpl, session: RemoteChatSession?)

    @ReflectedField("nextChatIndex")                     // field handle: every VarHandle mode
    val nextChatIndex: IntFieldHandle<ServerGamePacketListenerImpl>

    @ReflectedMethod("resetPlayerChatState")
    fun resetPlayerChatState(instance: ServerGamePacketListenerImpl, session: RemoteChatSession)
}

@ReflectionProxy(FilterMask::class)
interface V26_3FilterMaskReflection {

    @ReflectedConstructor
    fun createFilterMask(bitSet: BitSet): FilterMask
}
```

- One target class per interface, set with exactly one of `target` and `targetName`. `targetName`
  is loaded through the interface's class loader without initialising it.
- Every abstract method carries exactly one of `@ReflectedMethod`, `@ReflectedField` and
  `@ReflectedConstructor`. `@ReflectedStatic` goes next to a method or field binding. Default
  methods are left alone, `suspend` methods are rejected.
- An instance binding takes the receiver as its first parameter. Its type must be compatible with
  the target class; `Any` works.
- `name` defaults to the proxy method's JVM name, so a property getter needs an explicit name. A
  field handle property defaults to the property's name, read back from the getter by Kotlin's
  rule: `getXValue` looks for `xValue`, then `XValue`.

## Field handles

A `@ReflectedField` property whose type is a field handle interface (`field-handles.kt`) binds the
whole field instead of one read or write:

| Field | Instance | `@ReflectedStatic` |
| --- | --- | --- |
| `int` | `IntFieldHandle<R>` | `StaticIntFieldHandle` |
| `long` | `LongFieldHandle<R>` | `StaticLongFieldHandle` |
| `boolean` | `BooleanFieldHandle<R>` | `StaticBooleanFieldHandle` |
| any reference | `ObjectFieldHandle<R, T>` | `StaticObjectFieldHandle<T>` |

- Every abstract handle method is named after a `VarHandle.AccessMode` and calls it: `get`/`set`
  (plain), `getVolatile`/`setVolatile`, `getAcquire`/`setRelease`, `compareAndSet`, `getAndSet`,
  `getAndAdd` and `getAndBitwiseOr/And/Xor`. `addAndGet`, `getAndIncrement`, `incrementAndGet`,
  `getAndDecrement` and `decrementAndGet` are interface default methods over `getAndAdd`. There
  are no `float`, `double`, `short`, `byte` or `char` handles.
- The atomic modes are atomic only against other `VarHandle` or atomic accesses; the server's own
  plain `x++` still races with them.
- Validated at creation: the handle type must fit the field type exactly (`Int` needs `int`,
  `ObjectFieldHandle` any reference), `R` must be compatible with the target class, `T` must be
  the field type or one of its supertypes, the `Static` variant must match `@ReflectedStatic`,
  `access` must stay `GET`,
  and the field must not be `final`, because every handle exposes writes.
- `FieldHandleClasses` generates one hidden class per handle with the ClassFile API, next to the
  proxy interface. Each method `ldc`s the `VarHandle` from the class data (`classDataAt`) and
  calls the access mode with its exact descriptor; the handle in the class data has
  `withInvokeExactBehavior()`, so a wrong descriptor fails instead of adapting. When the proxy's
  package cannot name the receiver or field type, as for a package-private target, that type is
  erased to `Object` and the `VarHandle` keeps its default behaviour, which casts the reference
  without boxing.
- The proxy's property getter returns the handle as a `MethodHandles.constant`, so
  `proxy.nextChatIndex` folds to a constant in JIT-compiled code.
- Kotlin stores an annotation on an interface property without use-site target on a synthetic
  `get<Name>$annotations` method in `DefaultImpls`; the binder reads it there. `@get:` on the
  getter works as well.

## Matching

- Methods and fields are searched in the target class, its superclasses and all their interfaces,
  most specific first. Static-ness must match `@ReflectedStatic`.
- A method or constructor with exactly the proxy's parameter types wins. Otherwise exactly one
  candidate with the same arity whose parameter types are each assignable to or from the proxy's
  must exist; primitives match only their own wrapper. More than one is reported as ambiguous.
- The resolved handle is adapted with `MethodHandle.asType` to the proxy method's type, so the
  proxy may declare supertypes (`Any`, `List` for an `ObjectList` field) and casts happen at call
  time.
- Members are reached through `MethodHandles.privateLookupIn(declaringClass, ...)`. For a package
  its module does not open, such as `java.lang`, surf-api's own lookup is used and only public
  members are reachable.
- Writing a `final` field, by a `SET` function or a field handle, is unsupported and fails at
  creation. Use `unsafe` at the call site for
  the rare case, as `v1-21-11`'s item bridge does for `Item.components`.

## Lifecycle and failure

`ReflectionProxies.get(type)` creates the proxy on first use and caches it in a `ClassValue`, so
`reflectionProxy<T>()` and an injected bean return the same instance. Creation binds every method
before generating anything; all problems are collected into one `ReflectionProxyException` that
lists each method. A failed creation is not cached and is retried on the next call.

The implementation is one hidden class per interface, generated by `HandleInvokerClasses` (shared
with the event invokers and surf-nats): each method `ldc`s its `MethodHandle` from the class data
list and calls `invokeExact`, so calls are as fast as a direct `MethodHandle` constant and the
member's exceptions propagate unchanged. The hidden class lives in the interface's package. When a
private lookup from surf-api lacks full privilege there - the interface is in another module, such
as a plugin's class loader - `ProxyLookups` defines a small anchor class
`<interface>$ReflectionProxyLookup` next to the interface and uses its lookup.

## Beans

Every plugin context gets a lazy bean `<interface>#reflectionProxy` for each `@ReflectionProxy`
interface in the plugin's packages, so a proxy whose target is missing on this server fails only
when something injects it. surf-api's own code - the NMS version modules and Velocity's
`SuspendingEventHandler` - calls `reflectionProxy<T>()` instead; the root context registers no
proxy beans.
