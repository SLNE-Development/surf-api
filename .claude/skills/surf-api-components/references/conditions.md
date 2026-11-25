# Conditions

Sources: `foundation/spring/api/src/main/kotlin/dev/slne/surf/api/spring/ConditionalOnPlugin.kt`
(`ConditionalOnPlugin`, `PluginPresence`, `OnPluginCondition`) and the platform implementations
`PaperPluginPresence`, `VelocitySpringRuntime` and Minestom's `PluginManagerImpl`; for Spring
Boot's conditions, `ComponentScanning.kt` and `ContextFailureAnalysis.kt` in
`foundation/spring/impl/src/main/kotlin/dev/slne/surf/api/spring/context/`.

## The rule first

A condition decides whether Spring registers a bean definition at all. A bean whose condition does
not hold never exists, and an injection point that requires it fails with
`NoSuchBeanDefinitionException`, which does not name the condition. When a type that clearly
carries a stereotype is "not found", check its conditions first.

Say at the injection point that a dependency is optional: `T?`, `List<T>` or `ObjectProvider<T>`.

## `@ConditionalOnPlugin`

```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@JvmRepeatable(ConditionalOnPlugins::class)
@Conditional(OnPluginCondition::class)
annotation class ConditionalOnPlugin(vararg val value: String)
```

One annotation stands for one plugin: it holds when the plugin is installed under **any** of the
names, because a plugin's name can differ between platforms (`LuckPerms` on Paper, `luckperms` on
Velocity). Code in a module shared by several platforms lists every name. Repeat the annotation to
require several plugins; **every** repetition must hold. An annotation without a name fails. It goes
on a component class, a `@Configuration` class or a `@Bean` method.

```kotlin
@Service
@ConditionalOnPlugin("LuckPerms", "luckperms")
@ConditionalOnPlugin("PlaceholderAPI")
class PlaceholderPermissions : Permissions
```

The compiler collects repetitions into the container `ConditionalOnPlugins`; never write it by hand.

Spring evaluates it from bytecode metadata before it loads the annotated class. A bean that
extends or references an optional plugin's API therefore never causes a `NoClassDefFoundError`
when that plugin is missing. Write the integration as an adapter behind an interface of the
plugin's own:

```kotlin
import dev.slne.surf.api.spring.ConditionalOnPlugin
import net.luckperms.api.LuckPermsProvider
import org.springframework.stereotype.Service
import java.util.UUID

interface PrefixSource {
    fun prefix(player: UUID): String?
}

@Service
@ConditionalOnPlugin("LuckPerms")
class LuckPermsPrefixSource : PrefixSource {
    override fun prefix(player: UUID): String? =
        LuckPermsProvider.get().userManager.getUser(player)?.cachedData?.metaData?.prefix
}

@Service
class ChatFormatter(private val prefixes: PrefixSource?)
```

On a `@Bean` method, the optional plugin's types must not appear in the signature of a
configuration class that is always loaded; put the method into a `@Configuration` class that
carries the condition itself.

The named plugin must be a declared dependency of the plugin, a soft one when it is optional, so
the platform loads it first.

## `PluginPresence`

`PluginPresence` is `@InternalSurfApi`: the contract between `OnPluginCondition` and the platforms,
not something plugin code implements or calls. The platform registers one in every plugin context,
bootstrap and full, under
`PluginPresence.BEAN_NAME` (`surfPluginPresence`), before the refresh. Paper also registers one in
its root bootstrap context, after that context's own component scan, so beans of surf-api's Paper
root context may carry the condition, as the PlaceholderAPI hook's `PapiExpansionHolderFactory`
does; surf-api's own `@BootstrapComponent`s may not. Other root contexts have none, including a
root that hosts an application, such as a standalone service or a microservice, so
`@ConditionalOnPlugin` cannot be used there. What "installed" means:

| Platform | Answered by |
| --- | --- |
| Paper | Bukkit's plugin manager once the server exists; in the bootstrap phase, the plugins Paper registered. Both include the names under `provides`. |
| Velocity | `pluginManager.isLoaded`. The condition runs in the plugin's load phase, so only plugins Velocity created before count. |
| Minestom | the plugin manager's loaded plugins |

A condition evaluated in a context without a `PluginPresence` fails with "@ConditionalOnPlugin was
evaluated in a context without a PluginPresence".

## Spring's own conditions

Spring's `@Conditional(MyCondition::class)` with an
`org.springframework.context.annotation.Condition` works in every context. A condition runs before
any bean exists, so it reads only the `ConditionContext`: the bean factory's definitions, the
environment, the class loader.

```kotlin
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.stereotype.Service

class OnLeaderNode : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) =
        System.getenv("NODE_ROLE") == "leader"
}

@Service
@Conditional(OnLeaderNode::class)
class ScheduledTasks
```

## Spring Boot's conditions

Every runtime ships `spring-boot` and `spring-boot-autoconfigure`, so plugin contexts, the root and
standalone programs evaluate Boot's conditions without running Spring Boot itself. Compile against
them with `compileOnly`, as against Spring. Design and reasons: `docs/spring-migration.md`, section
"Spring Boot conditions". Tests check conditions with `surfContextRunner()`; see the
**surf-api-standalone** skill.

| Condition | Behaviour in a surf context |
| --- | --- |
| `@ConditionalOnClass`, `@ConditionalOnMissingClass` | read the plugin's class loader; use `name = [...]` for a class that may be missing |
| `@ConditionalOnBean`, `@ConditionalOnMissingBean`, `@ConditionalOnSingleCandidate` | the default `SearchStrategy.ALL` sees the plugin's beans, its direct dependencies' exports and the root; `CURRENT` only the plugin's own context; `ANCESTORS` fails in a root context |
| `@ConditionalOnProperty`, `@ConditionalOnExpression` | read system properties and environment variables only; there is no `application.yml` and no property source per plugin |
| `@ConditionalOnResource` | reads the plugin's class loader; on Paper only the plugin's own jar |
| `@ConditionalOnJava`, `@ConditionalOnThreading`, `@ConditionalOnCloudPlatform` | as in Spring Boot; `@ConditionalOnThreading(VIRTUAL)` reads `spring.threads.virtual.enabled` |
| `SpringBootCondition`, `AnyNestedCondition`, `AllNestedConditions`, `NoneNestedConditions` | base classes for your own conditions; `ConditionContext.classLoader` is the plugin's |
| `@ConditionalOnWarDeployment` | unsupported: it needs `spring-web`, which no runtime ships |

```kotlin
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CacheConfiguration {

    @Bean
    @ConditionalOnClass(name = ["com.github.benmanes.caffeine.cache.Caffeine"])
    @ConditionalOnMissingBean
    fun priceCache(): PriceCache = CaffeinePriceCache()
}
```

A bean condition sees only the definitions registered before it runs. Scanned components register
first, then the `@Bean` methods and the imported auto-configurations in their order.

### `@ConditionalOnMissingBean` belongs on `@Bean` methods

On a scanned class the condition always removes the class: Spring evaluates it after the scan
registered the class, so the class matches itself. surf-api therefore fails the context's start
with `The scanned classes [...] carry @ConditionalOnMissingBean ...` when a scanned class carries
it, directly or as a meta-annotation. A default that a real implementation replaces carries
`@Fallback` instead; a conditional default goes into a `@Bean` method. The check covers surf-api's
component scan only: a `@ComponentScan` you declare yourself uses Spring's plain scanner, which
lets such a class remove itself silently and registers `@AutoConfiguration` classes as ordinary
components.

### Auto-configurations

Import auto-configurations by class; discovery from
`META-INF/spring/*.AutoConfiguration.imports` is unsupported, because Paper's plugin class loader
returns only the plugin's own jar from `getResources`.

```kotlin
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Configuration

@Configuration
@ImportAutoConfiguration(classes = [StorageAutoConfiguration::class, CacheAutoConfiguration::class])
class Imports
```

`@AutoConfigureBefore`, `@AutoConfigureAfter`, `@AutoConfigureOrder` and
`@AutoConfiguration(before = ..., after = ...)` order the imported classes whatever order the list
names them in. A class annotated with `@AutoConfiguration` is never component-scanned, even inside
the plugin's base package; only an import applies it.

### Failure analysis

When a context fails to start, surf-api runs Spring Boot's failure analyzers before it closes the
context. A recognized failure arrives as `ContextFailureAnalyzedException`, inside the
`PluginContextException` for a plugin context and directly from a failed root context; its message
holds Boot's `Description:` and `Action:`, for example for a `NoUniqueBeanDefinitionException` or a
`NoSuchBeanDefinitionException`, including the conditions that skipped a candidate. Enable debug
logging for `dev.slne.surf.api.spring.context.ContextFailureAnalysis` to also log the failed
context's condition evaluation report.

## Defaults and ordering without a condition

- A default that a real implementation replaces carries Spring's `@Fallback`; any other candidate
  wins over it.
- A bean that must start after another without injecting it carries Spring's
  `@DependsOn("beanName")`. Constructor parameters already order everything a bean uses.
