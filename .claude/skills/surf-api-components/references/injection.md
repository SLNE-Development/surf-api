# Injection in detail

Sources: `foundation/spring/impl/.../spring/context/ImportContexts.kt` (`PluginBeanFactory`,
`ImportBeanFactory`), `PluginExport.kt`, `SurfContextManager.kt`, and the cases in
`foundation/spring/impl/src/test/kotlin/dev/slne/surf/api/spring/context/SurfContextManagerTest.kt`.
Everything not named there is plain Spring Framework behaviour.

## Where candidates come from

A plugin context answers from its own beans first, then asks its import context, then the root.
The import context holds one lazy, synthetic definition per export of a direct dependency, named
`import/<provider>/<bean name>`. The `/` cannot occur in a parameter name, so Spring's fallback of
matching a parameter name against bean names never picks an export by accident.

An export carries the bean's full generic type. An export declared as `Repo<Foo>` never satisfies
a `Repo<Bar>` parameter.

## The parameter shapes

### `T` - exactly one

Spring collects the candidates of this context and its ancestors. One candidate is injected. Among
several, Spring narrows by `@Qualifier`, then takes the `@Primary` one, then the only one left
without `@Fallback`, then the one whose bean name equals the parameter name (kept in the bytecode
by `javaParameters`); anything still ambiguous fails with `NoUniqueBeanDefinitionException`. No candidate fails with
`NoSuchBeanDefinitionException`. Both arrive wrapped in `UnsatisfiedDependencyException`.

### `T?` - optional

A nullable Kotlin parameter is not required: Spring passes `null` when nothing provides `T`. It
does not soften ambiguity; two candidates still fail. `Optional<T>` behaves the same.

### `List<T>` / `Set<T>` / `Map<String, T>` - every candidate

Every candidate of this context and its ancestors, root beans and imported exports included.
`List<T>` is sorted by `@Order` or `Ordered`. A class whose only constructor takes the collection
receives an empty one when nothing provides `T`. Collections never fail on ambiguity, including
exports from several providers.

### `ObjectProvider<T>` - resolved on use

Nothing is resolved at construction:

- `getObject()` - exactly one, as for `T`
- `getIfAvailable()` - one or `null`; still fails on ambiguity
- `getIfUnique()` - one, or `null` when there is none **or** several
- `orderedStream()` / `stream()` - every candidate

Use it to break a constructor cycle, to reach a lazy bean only when needed, or to look a bean up
after the plugin's phases ran. A NATS client or service proxy, or a `RedisApi`, created through an
`ObjectProvider` after the context refreshed connects late or fails; see the surf-api skills for
those integrations.

### `@Lazy T` - a resolving proxy

Spring's `@Lazy` on an interface-typed parameter injects a proxy that resolves the bean on its
first call. It breaks a constructor cycle like `ObjectProvider<T>`, with the call site unchanged.

## `@Bean` methods

```kotlin
@Configuration
class StorageConfiguration {

    @Bean
    @Primary
    fun storage(settings: ConfigHandle<StorageSettings>): Storage = DatabaseStorage(settings.value.url)
}
```

- A `@Bean` method belongs in a `@Configuration` class. `kotlin("plugin.spring")` opens it, so
  Spring creates its CGLIB subclass, and one `@Bean` method calling another returns the singleton.
- The declared return type is the type Spring predicts before the bean exists, which type matching
  of lazy beans relies on, and the type an export carries to consumers. Write the contract there.
- Parameters are resolved like constructor parameters, in every shape above.
- `@Qualifier`, `@Primary`, `@Fallback`, `@Order`, `@Lazy` and `@ConditionalOnPlugin` work on the
  method.

## Qualifier, primary, fallback, order

```kotlin
@Service @Qualifier("file") class FileStorage : Storage
@Service @Qualifier("memory") @Fallback class MemoryStorage : Storage

@Service
class Writer(@Qualifier("file") private val storage: Storage)
```

- A qualifier at the injection point narrows the candidates to that name. A name that matches
  nothing means "no candidate": a `T` parameter fails and a `T?` parameter receives `null`, so a
  typo in a qualifier looks like a missing bean.
- `@Primary` decides an unqualified injection point; two primaries for one type fail.
- `@Fallback` beans count only when no other candidate is left. A default implementation that a
  real one replaces carries `@Fallback`. surf-api's Redis and NATS beans are registered this way,
  so a plugin's own `NatsClient` or `RedisApi` bean wins.
- `@Order` sorts collections, `orderedStream()` and the lifecycle hooks. It never picks a single
  candidate. On a `@Bean` method it sorts collections only; the lifecycle hooks read `@Order` or
  `Ordered` from the bean's class.

## Exports from several providers

A plugin context treats candidates from two different providers as undecidable for a single-valued
injection point:

- `T`, `T?`, `Optional<T>` and `ObjectProvider.getIfAvailable()` fail with
  `NoUniqueBeanDefinitionException`, even when one export is `@Primary`, carries a higher priority,
  or its bean name matches the parameter name.
- `ObjectProvider.getIfUnique()` returns `null`.
- A programmatic `getBean(Type::class.java)` fails the same way.
- `@Qualifier("<value>")` at the injection point picks one. Only a plain `@Qualifier` value travels
  with an export; a custom qualifier annotation stays behind.

Within one provider, and among the plugin's own beans, Spring's usual rules apply.

## Programmatic lookups

Inject what a bean needs. Code that holds an `ApplicationContext` uses Spring's reified Kotlin
extensions from `org.springframework.beans.factory`:

```kotlin
val shop = context.getBean<ShopService>()
val handlers = context.getBeansOfType<PriceHandler>().values
```

A plugin bean can inject `ApplicationContext` itself. `getBeansOfType` looks only at the context it
is called on, not at its ancestors.

surf-api's own static entry points read root beans through the `@InternalSurfApi` delegate
`surfBean<T>(qualifier)` in `dev.slne.surf.api.spring`:

```kotlin
private val bridge by surfBean<SurfPaperNmsNbtBridge>()
```

It resolves on first read from `SurfContexts.root`. A read before the platform installed the root
fails with "surf-api's root context has not been started yet".
