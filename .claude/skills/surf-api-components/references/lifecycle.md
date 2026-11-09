# Lifecycle and post processors

Sources: `foundation/di/api/.../SurfComponent.kt`, `BeanPostProcessor.kt`, `Scope.kt`.

## The phases

The container runs each phase as a separate pass over all beans, each pass in dependency order:

1. **construct** - every bean is created and injected
2. **post-process** - every bean passes through the `BeanPostProcessor` chain
3. **`load()`** on every bean
4. **`enable()`** on every bean
5. **`disable()`** on every bean, in reverse

Separate passes, not per-bean. Every bean's `load` has run before the first `enable`, and every bean
exists before the first `load`.

```kotlin
interface SurfComponent {
    suspend fun load() = Unit
    suspend fun enable() = Unit
    suspend fun disable() = Unit
}
```

All three suspend, all three default to doing nothing, and all three are
`@ApiStatus.OverrideOnly` - override what you need and never call them yourself. They run in the
container's scope.

## Why `load` and `enable` are two phases

Because a resource that others register against cannot be opened and frozen at the same moment.

- `load` - create or open the thing. Beans that will register against it may not have loaded yet, so
  it has to tolerate registrations arriving after this returns.
- `enable` - every `load` has run, so nothing more will register: freeze, connect, go live.

A command registry fills during `load` and is published in `enable`. A connection is constructed in
`load` and dials out in `enable`. If you put both halves into one method, either the registrations
arrive after the freeze or the connection is live before the handlers exist.

`disable` in reverse means a bean is torn down before what it depends on: a client closes before its
connection, a handler unregisters before the broker goes away.

A phase that throws stops that phase, and the container reports which bean failed. During shutdown
it is the other way round: a failing `disable` is reported and the remaining beans are still
disabled, because a half-shut-down process is worse than a logged failure.

## What `PROTOTYPE` opts out of

A `PROTOTYPE` bean is created fresh for every injection point and never cached, so the container does
not hold it long enough to shut it down. It therefore takes **no part in the lifecycle** - `load`,
`enable` and `disable` are never called on it - and it is not among the beans the container creates
at startup, so it does not pass through the post processor chain either. Anything that has to be
opened, closed, registered or post-processed must not be a `PROTOTYPE`.

## `BeanPostProcessor`

```kotlin
interface BeanPostProcessor {
    suspend fun afterInitialization(bean: Any, name: String): Any = bean
    suspend fun beforeDestruction(bean: Any, name: String) = Unit
}
```

`afterInitialization` runs for every bean after construction and injection, before any lifecycle
phase. `beforeDestruction` runs for every bean during shutdown, before its `disable`, in reverse
creation order - undo there what `afterInitialization` registered.

`name` is the bean's id, for messages. The return value is the bean to use from then on.

### What it is for

Acting on beans that nothing collected, where the thing identifying them is not a type. If handlers
were identified by implementing an interface, a `List<Handler>` parameter would do the job and no
processor would be needed. A processor earns its place when the marker sits on a **method**, on an
annotation, or on a supertype that the registering side must not depend on.

### Worked example: registering message handlers

`MessageBroker` and `@MessageHandler` here are the project's own types.

```kotlin
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation

@Service
@Priority(-100)
class MessageHandlerRegistrar(private val broker: Lazy<MessageBroker>) : BeanPostProcessor {

    override suspend fun afterInitialization(bean: Any, name: String): Any {
        if (bean.handlesMessages()) broker.value.registerHandler(bean)
        return bean
    }

    override suspend fun beforeDestruction(bean: Any, name: String) {
        if (bean.handlesMessages()) broker.value.unregisterHandler(bean)
    }

    private fun Any.handlesMessages() =
        this::class.declaredMemberFunctions.any { it.hasAnnotation<MessageHandler>() }
}
```

### The two rules

**The dependency runs one way only.** The handler beans know the broker; the broker knows nothing
about them; the registrar sits beside both and is the only place that knows both. Collecting the
handlers into the broker's constructor instead inverts that: the broker would wait for every
handler, and the first handler that needs the broker closes a cycle.

**Take the registration target as `Lazy<T>`.** A processor is created before the beans it processes.
Demanding the broker eagerly would force it to be created before the processor can run, which the
container cannot promise for every ordering; `Lazy` postpones it to the first bean that actually
needs registering.

### Rules the container enforces

- A processor is an ordinary bean: annotate it, give it constructor parameters, scope it.
- A processor never sees itself or another processor. Processors are created first and separately,
  precisely so they exist before the first bean passes through the chain.
- `@Priority` on a processor orders the chain, lower first: every bean passes through the processors
  in that order.
- Returning a different object from `afterInitialization` replaces the bean from then on - the
  instance cache and the lifecycle list both follow the replacement. Only do this when wrapping is
  the point. Returning something that does not satisfy the injection points expecting the bean fails
  at the first use, not here.
