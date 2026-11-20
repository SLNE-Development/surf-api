# Conditions

Sources: `foundation/spring/api/src/main/kotlin/dev/slne/surf/api/spring/ConditionalOnPlugin.kt`
(`ConditionalOnPlugin`, `PluginPresence`, `OnPluginCondition`) and the platform implementations
`PaperPluginPresence`, `VelocitySpringRuntime` and Minestom's `PluginManagerImpl`.

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
`PluginPresence.BEAN_NAME` (`surfPluginPresence`), before the refresh. Root contexts have none,
including a root that hosts an application, such as a standalone service or a microservice, so
`@ConditionalOnPlugin` cannot be used there. What "installed" means:

| Platform | Answered by |
| --- | --- |
| Paper | Bukkit's plugin manager once the server exists; in the bootstrap phase, the plugins Paper registered. Both include the names under `provides`. |
| Velocity | `pluginManager.isLoaded`. The condition runs in the plugin's constructor, so only plugins Velocity constructed before count. |
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

Spring Boot's `@ConditionalOnProperty`, `@ConditionalOnClass` and `@ConditionalOnMissingBean`
exist only in microservices, which run on Spring Boot. Paper, Velocity, Minestom and the standalone
runtime ship Spring Framework without Boot.

## Defaults and ordering without a condition

- A default that a real implementation replaces carries Spring's `@Fallback`; any other candidate
  wins over it.
- A bean that must start after another without injecting it carries Spring's
  `@DependsOn("beanName")`. Constructor parameters already order everything a bean uses.
