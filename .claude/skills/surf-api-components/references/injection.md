# Injection in detail

Sources: `foundation/di/api/.../BeanContainer.kt`, `Bean.kt`, `Qualifier.kt`, `Primary.kt`,
`Priority.kt`, `definition/BeanDefinition.kt`.

## What a bean can be resolved as

A bean's `provides` list is its own class plus every supertype except `Any`. That is what makes
resolution over an interface work, and it means a bean is reachable under any of those types without
anything being declared twice.

Consequence: a bean that implements a widely used interface becomes a candidate for every injection
point asking for it. Two such beans and no `@Primary` is an ambiguity failure, not a coin toss.

## The four parameter shapes

The container records each constructor parameter as an injection point with the declared type plus
three flags - optional, collection kind, lazy - and the flags decide how it is resolved.

### `T` - exactly one

Resolved from this container's own candidates first; if none, the parent is asked, then its parent.
No candidate anywhere is a failure that names the consumer and the type. Several candidates in one
container with nothing to pick between them is an ambiguity failure.

### `T?` - optional

A nullable parameter accepts absence: the whole hierarchy is searched, and `null` is passed if
nothing provides the type. Use it instead of a condition when the bean works fine without the
dependency. An optional parameter that nothing provides also creates no graph edge.

### `List<T>` / `Set<T>` - every candidate

The union of this container and its parents:

1. this container's candidates, sorted by `@Priority` (lower first) and then by provider id, so the
   order is stable across runs whatever order the metadata files arrived in
2. then the parent's candidates, recursively, by the same rule

`Set<T>` is the same list turned into a set. An empty collection is an answer, not a failure - a
registrar with no handlers starts normally.

A collection parameter creates an edge to **every** provider of its element type, so all of them
exist before the collection is handed over.

Do not write `Lazy<List<T>>`: a parameter is one shape or the other, and the collection branch is
taken first, so the laziness would be silently lost.

### `Lazy<T>` - resolved on first access

The value is produced the first time `.value` is read. Two uses:

- **breaking a cycle on purpose.** A lazy parameter creates no graph edge, which is exactly why it
  is the answer to a cyclic-dependency failure. Make the parameter lazy on the side that genuinely
  uses the value later than construction.
- **holding something that does not exist yet** - the registration target of a
  `BeanPostProcessor`, for instance.

A required `Lazy<T>` whose bean does not exist fails at **first access**, not at startup. That is the
price of postponing it.

## `@Bean` factories

```kotlin
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
annotation class Bean(val scope: Scope = Scope.SINGLETON)
```

- Works on a property (read once) or a function (called once, with its parameters resolved like any
  other injection point).
- The declaring class must itself be a bean. The factory is created after its declaring bean; the
  graph enforces that, so a consumer of the published type transitively waits for the declaring bean
  too.
- **The published type is the declared type** of the property or the return type of the function -
  not the runtime class of the value. Write the contract explicitly:

  ```kotlin
  @Bean val api: RedisApi = RedisApiImpl()        // publishes RedisApi
  @Bean val api = RedisApiImpl()                  // publishes RedisApiImpl - consumers of RedisApi fail
  ```

  The published bean is indexed under that declared type and its supertypes, the same as a class
  bean.
- `scope` defaults to `Scope.SINGLETON`. Set it explicitly when the published value should live in
  the root container: `@Bean(scope = Scope.SHARED)`.
- A factory can carry `@Qualifier`, `@Primary` and `@Priority`; those are recorded per factory.
  Conditions are not - put a `@ConditionalOn*` on the declaring class instead
  (see `references/conditions.md`).
- In failure messages a factory is identified as `DeclaringClass#memberName`.

## Qualifier, primary, priority

```kotlin
annotation class Qualifier(val name: String)   // class, value parameter, property, function
annotation class Primary                       // class, property, function - not a parameter
annotation class Priority(val value: Int = 0)  // class, annotation class, property, function
```

- An injection point without a qualifier considers **all** candidates for the type, qualified ones
  included; a qualifier on a bean does not hide it. It then takes the only candidate, or the
  `@Primary` one, or fails.
- A qualifier at the injection point narrows to candidates with that exact name. A name that matches
  nothing is treated as "no candidate" - so a required parameter fails and a nullable one gets
  `null`. A typo in a qualifier therefore looks like a missing bean.
- At most one `@Primary` per provided type. Two of them fail rather than one being picked.
- `@Priority` is only a tie-breaker between beans the graph leaves unordered - two independent
  listeners whose registration order happens to matter. It also sets the order of candidates handed
  to a collection parameter, and the order of the `BeanPostProcessor` chain. It can never override
  the graph.
- `@Priority` on an annotation class applies to every bean carrying that stereotype.

## Reaching the container directly

Only at the edges - a platform entry point, a static callback, somewhere with no constructor to
inject into. Inside a bean, use the constructor.

```kotlin
interface BeanContainer {
    val parent: BeanContainer?
    val owner: Any

    fun <T : Any> beanOrNull(type: KClass<T>, qualifier: String? = null): T?
    fun <T : Any> bean(type: KClass<T>, qualifier: String? = null): T
    fun <T : Any> beansOfType(type: KClass<T>): List<T>
    fun <T : Any> lazyBean(type: KClass<T>, qualifier: String? = null): Lazy<T>
}
```

Reified extensions exist for all four, so the usual call is
`container.bean<ShopService>()` / `container.beansOfType<PriceHandler>()` /
`container.lazyBean<RedisApi>()` / `container.beanOrNull<Storage>("fallback")`.

- `bean` throws when there is no candidate, or when several match and none is `@Primary`. Both
  messages name what was found.
- `beanOrNull` returns `null` for "none", but still throws on ambiguity - absence and
  undecidability are different problems.
- `beansOfType` returns this container's and its parents' candidates; empty is a valid answer.
- `lazyBean` resolves on first access, which is how a platform entry point can hold a handle to a
  bean that does not exist yet.

`owner` is the application the container belongs to - a plugin instance, a microservice. `parent` is
`null` only on the root.
