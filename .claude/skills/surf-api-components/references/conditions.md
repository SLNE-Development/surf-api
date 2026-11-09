# Conditions

Source: `foundation/di/api/src/main/kotlin/dev/slne/surf/api/di/condition/conditions.kt`.

## The rule first

A condition is evaluated at startup, before the bean exists. If it does not hold, the bean is
**skipped**: never constructed, never injected. An injection point that requires it then fails with
the unsatisfied-dependency message, which names the consumer and the type and tells you to check
whether a condition skipped the provider - it does not name the condition itself, so when a type
"no bean provides" clearly is annotated, the condition is the first thing to look at. A missing bean
is treated as a configuration error on purpose.

So: if a dependency is genuinely optional, say so at the injection point with `T?` (or `List<T>`,
which may be empty). Do not guard the consumer with the same condition as the dependency and hope
the two stay in step.

Every annotation below is `@Repeatable` and targets `CLASS`, `ANNOTATION_CLASS`, `PROPERTY` and
`FUNCTION`. Repeating one means every copy has to hold, which is how several of them narrow a set
rather than widen it. Annotating an **annotation class** puts the condition on every bean carrying
that stereotype.

Guard the declaring class, not the member: conditions are recorded per bean definition, and a
`@Bean` factory's definition (`definition/BeanDefinition.kt`, `FactoryDefinition`) carries no
conditions of its own. A condition on a `@Bean` property or function has nowhere to be recorded, so
do not rely on it - move the condition to the class that declares the factory, or move the factory
onto a class you can guard.

## The family, with exact signatures

```kotlin
annotation class ConditionalOnProperty(
    val key: Array<String>,
    val havingValue: String = "",
    val matchIfMissing: Boolean = false,
    val file: String = "",
)
```

`key` is the path to the value, segment by segment. With `havingValue` set, the value has to equal it
ignoring case; left empty, the property only has to be present. `matchIfMissing` says what an absent
property counts as - that is how a feature defaults to on or off. `file` names the configuration file
relative to the application's data directory; empty means the default file.

```kotlin
@Service
@ConditionalOnProperty(key = ["shop", "enabled"], havingValue = "true", matchIfMissing = true)
class ShopService(private val transactions: TransactionApi)
```

```kotlin
annotation class ConditionalOnEnvironment(val environments: Array<String>)
```

Holds if the process runs in one of the named environments. The environments come from the platform.
Two annotations mean both groups have to match.

```kotlin
annotation class ConditionalOnClass(val clazz: KClass<*>)
annotation class ConditionalOnClassName(val className: String)
```

For an integration with an optional library. `@ConditionalOnClass` needs the class at compile time
(the annotation references it); `@ConditionalOnClassName` is the string form for when even that is
unavailable - the usual case for a soft dependency compiled against nothing.

```kotlin
annotation class ConditionalOnPlugin(val pluginId: String)
annotation class ConditionalOnAnyPlugin(val pluginIds: Array<String>)
```

Whether the platform has that plugin loaded. `@ConditionalOnAnyPlugin` holds when at least one of
the ids is loaded - for an integration that works with any of several interchangeable plugins.
Repeating `@ConditionalOnPlugin` requires all of them.

```kotlin
annotation class ConditionalOnMissingBean(val type: KClass<*>)
```

Holds only when no other bean provides that type. This is how a default is written: mark the fallback
with it, and any real implementation that appears takes over without the fallback knowing about it.
Assignability decides, so a fallback guarded on an interface steps aside for anything implementing
it.

```kotlin
@Service
@ConditionalOnMissingBean(Storage::class)
class InMemoryStorage : Storage
```

```kotlin
annotation class ConditionalOn(val condition: KClass<out BeanCondition>)
```

Runs your own condition. Repeatable, and all of them have to hold.

```kotlin
annotation class DependsOnBean(val type: KClass<*>)
```

**Not a condition.** It orders this bean after the bean providing that type, without injecting it -
for the rarer case where the order matters but the value does not, and the bean never touches it.
Assignability decides, so the type may be an interface. Constructor parameters already express every
dependency the bean actually uses, so reach for this only when there is nothing to inject.

## Writing a `BeanCondition`

```kotlin
interface BeanCondition {
    suspend fun evaluate(context: BeanConditionContext): Boolean
}

class BeanConditionContext(
    val owner: Any,
    val container: BeanContainer,
    val beanClassName: String,
)
```

```kotlin
class OnLeaderNode : BeanCondition {
    override suspend fun evaluate(context: BeanConditionContext) =
        System.getenv("NODE_ROLE") == "leader"
}

@Service
@ConditionalOn(OnLeaderNode::class)
class ScheduledTasks
```

- The implementation needs a constructor the container can call **with no arguments**. A condition
  runs before the beans it decides about exist, so it cannot inject them.
- `context.container` is the container being built - ask it only about what is already known, never
  about the bean being decided.
- `context.owner` is the application (a plugin instance, a microservice); `beanClassName` is the
  guarded bean, for messages.
- `evaluate` suspends, so a condition may do real I/O - but it runs during startup, so keep it
  cheap.
