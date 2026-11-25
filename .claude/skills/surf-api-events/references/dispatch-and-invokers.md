# Dispatch internals and invoker generation

Source: `common/core/impl/src/main/kotlin/dev/slne/surf/api/core/server/impl/event/` -
`SurfEventBusImpl.kt`, `SurfEventHandlerEntry.kt`, `invoker/` - plus
`InvokerFactory.kt`, `HandleInvokerClasses.kt` and `InvokerMethods.kt` in
`common/common/api/src/main/kotlin/dev/slne/surf/api/common/invoker/`.

Read this when you need to reason about ordering, performance or why a handler did or did not run.
None of it is public API; `SurfEventBus` is `@ApiStatus.NonExtendable`.

## Registration state

`SurfEventBusImpl` keeps

```
handlers: ConcurrentHashMap<Class<out SurfEvent>,
              ConcurrentHashMap<SurfEventPriority, CopyOnWriteArrayList<SurfEventHandlerEntry>>>
dispatchCache: ConcurrentHashMap<Class<out SurfEvent>, Array<SurfEventHandlerEntry>>
nextOrder: AtomicLong
```

keyed by the **declared** event type - the handler's parameter type, or the `eventClass` passed to
`registerHandler`. Every entry gets a monotonically increasing `order` from `nextOrder`, which is
the tiebreaker between handlers of equal priority.

`SurfEventHandlerEntry` is a sealed interface with `SyncInvoker` and `AsyncInvoker` implementations
carrying `token`, `priority`, `ignoreCancelled`, `order` and the invoker. Its derived property

```kotlin
val skipWhenCancelled get() = ignoreCancelled && priority != SurfEventPriority.MONITOR
```

is the whole of the cancellation policy: `MONITOR` can never be skipped.

The `token` is the listener instance for annotated handlers, and a freshly allocated `Any()` for
lambda handlers - which is why `registerHandler`/`on` hand that object back and why
`unregisterListeners` compares with `===`.

Any registration or unregistration clears `dispatchCache` wholesale.

## Building the dispatch list

`collectMatching(concreteType)` fills the cache through `buildMatchingHandlers`:

1. walk every registered event type, keep those where `registeredType.isAssignableFrom(concreteType)`
2. collect all their handler lists
3. sort by `priority.ordinal`, then by `order`
4. store as an `Array`, return `null` when empty so dispatch can bail out early

So polymorphic dispatch is plain `isAssignableFrom`, and a handler registered for a supertype is
merged into the same sorted list as one registered for the concrete type - priority wins over
specificity.

The broadest parameter type you can actually register is `SurfSyncEvent` or `SurfAsyncEvent`
itself. A handler declared for `SurfEvent` passes the "is a `SurfEvent`" check and then falls into
`registerMethodHandler`'s `else` branch, which throws "is not a SurfSyncEvent or SurfAsyncEvent".

The cache is keyed on the concrete runtime class, so the sort happens once per event class per
registration generation; the hot path is an array walk.

## Invocation

```kotlin
// callSync
for (handler in matching) {
    if (handler.skipWhenCancelled && cancellable?.isCancelled == true) continue
    try { (handler as SyncInvoker).invoker.invoke(event) }
    catch (t: Throwable) { log.atSevere().withCause(t).log(…) }
}
```

`callAsync` is the same loop plus `currentCoroutineContext().ensureActive()` before each handler,
and a special case in the catch:

- a `CancellationException` from a handler re-checks `ensureActive()`. If the publishing coroutine is
  still active, the exception is logged at WARNING ("cancelled itself") and dispatch **continues**.
  If the publishing coroutine really was cancelled, `ensureActive()` rethrows and dispatch stops.
- anything else is logged at SEVERE and dispatch continues.

Both return the same event instance. Neither ever propagates a handler's exception to the publisher.

The `handler as SyncInvoker` / `as AsyncInvoker` casts are safe because the sync and async event
hierarchies are disjoint: a `SurfSyncEvent` can only match types registered through the sync path.

## The generated invokers

`SurfSyncEventInvoker` and `SurfAsyncEventInvoker` are `fun interface`s - `invoke(event)` and
`suspend invoke(event)`. Implementations are **not** reflective calls and not lambdas: they are
hidden classes.

`SurfEventInvokerFactory` holds one `InvokerFactory` per flavour, built from the invoker
interface alone:

```kotlin
private val syncFactory = InvokerFactory(SurfSyncEventInvoker::class.java, lookup)
```

`InvokerFactory` (`common/common/api/.../invoker/`) generates one class template with the ClassFile
API: a class implementing the interface's single method by calling a `MethodHandle` loaded from its
class data with `ldc` and `invokeExact`. `createSync` and `createAsync` adapt the handler into one
such handle and define a fresh hidden class per handler with that handle as class data.
`HandleInvokerClasses`, the generator underneath, is shared with surf-nats' listener and service
invokers and with the reflection proxies.

Consequences:

- Dispatch is an `invokeExact` on a constant, bound handle - no `Method.invoke`, no argument array.
- The handle is wrapped in `MethodHandles.guardWithTest` on `eventType.isInstance(event)`, so a
  mismatch is a silent no-op rather than a `ClassCastException`.
- For the async flavour a non-suspend handler is adapted to return `Unit` and ignore the
  continuation. That is why a **non-suspending** method is a valid `SurfAsyncEvent` handler.
- Access is checked up front with `MethodHandles.privateLookupIn(declaringClass, lookup)`
  (`InvokerMethods.canAccess`), which is why private handler methods work on the classpath, and
  why a listener in a named JPMS module that is not open to surf-api fails registration with
  `IllegalAccessException`.
- The handler's own exceptions propagate unchanged through the invoker.

`InvokerMethods.isSuspendFunction` is shared with Velocity's `SuspendingEventHandler`, so fix
suspend detection there, not per caller.

## Handler discovery

`collectHandlerMethods(type)` walks `declaredMethods` from the listener class up to - but not
including - `Any`, skipping synthetic and bridge methods, and keeps everything annotated with
`@SurfEventHandler`. `validateAndExtractEventType` then enforces the signature rules and strips the
trailing `Continuation` parameter from the count for suspend functions.

Because the walk includes superclasses, a handler declared on an open base class is registered for
every subclass instance that is registered - and if both a base method and an override carry the
annotation, both registrations are created.
