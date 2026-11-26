# Commands on Paper and Velocity

Both platforms use **upstream CommandAPI 12.0.0** — `dev.jorel:commandapi-paper-plugin` /
`commandapi-velocity-core` plus the Kotlin DSL artifacts, declared in `gradle/libs.versions.toml` as
`commandapi = "12.0.0"` (`commandapiplugin = "12.1.0"` for the plugin itself). The builders, the
argument base class, `CommandArguments`, `CustomArgument`, `ArgumentSuggestions` and the
`commandAPICommand` / `commandTree` / `subcommand` DSL all come from `dev.jorel.commandapi` — read
upstream's documentation for those. surf only adds the pieces below.

`platform/paper/api/build.gradle.kts` has `compileOnlyApi(libs.commandapi.paper)` and
`api(libs.commandapi.bukkit.kotlin)`; the Velocity module the equivalent pair. On Paper the
`dev.slne.surf.api.gradle.paper-plugin` Gradle plugin adds `CommandAPI` to the generated
`paper-plugin.yml` as a required server dependency with join-classpath, loaded before the plugin:
Paper does not pass CommandAPI's classes on through surf-api, so without it the plugin cannot load
its command classes.

Command beans - classes extending `CommandAPICommand` or `CommandTree`, and `CommandRegistrar`
beans - are registered in the plugin's enable phase by `CommandApiCommandBeans`
(`common/core/impl/.../core/server/spring/`). See the main skill's "Registering a command".

## Shared, in `surf-api-command-api`

`common/command/api/src/main/kotlin/dev/slne/surf/api/command/` — its own module, built against
`compileOnlyApi(libs.commandapi.core)`, so it is for Paper and Velocity. **Minestom's module
explicitly excludes `commandapi-core`, so none of this except `CommandRegistrar` is usable from
Minestom code.**

| File | Holds |
| --- | --- |
| `CommandRegistrar.kt` | `fun interface CommandRegistrar { fun register() }` - a bean that registers commands in the enable phase; names no CommandAPI type, so Minestom uses it too |
| `SurfCommandUtil.kt` | `createException(message: ComponentLike)`, `failWithMessage(message)`, `failWithBuilder(builder)` — all `@JvmStatic` on an `object`; the two `fail*` functions return `Nothing` |
| `exception/WrapperCommandExceptionComponent.kt` | a `WrapperCommandSyntaxException` that keeps its `Component` in `component`; its Brigadier message comes from the platform's `CommandMessageFactory`, so the client sees the component |
| `exception/CommandMessageFactory.kt` | `@InternalSurfApi fun interface` turning a `Component` into the platform's Brigadier `Message`. A root bean on Paper (`BukkitTooltip.messageFromAdventureComponent`) and Velocity (`VelocityBrigadierMessage.tooltip`); Minestom and standalone have none, because they exclude `commandapi-core` |
| `builder/CommandExceptionBuilder.kt` | formats a parse failure: optional detail message, then `At position <cursor>: ` plus the last `CONTEXT_AMOUNT = 10` characters underlined and a `command.context.here` marker. `build()` uses `Colors.ERROR_PREFIX`; `build(prefix)` takes another |
| `args/SuspendArgumentHelper.kt` | `suspend CommandArguments.awaiting<T>(nodeName)` and `awaitingOrNull<T>(nodeName)` — `await()` a `Deferred<T>` stored as an argument value; `awaiting` errors with *"Argument … is missing"* |

`CommandExceptionBuilder` is the one piece both platforms' `MiniMessageArgument` share.

## Paper additions

`platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/command/`.

### Arguments (`args/`)

| Class | Shape |
| --- | --- |
| `MiniMessageArgument(nodeName, greedy = false)` | `CustomArgument<Component, String>` over a `GreedyStringArgument` or `TextArgument`; a `ParsingException` becomes a `CustomArgumentException.fromAdventureComponent(CommandExceptionBuilder(…).build())`. DSL: `CommandAPICommand.miniMessageArgument(nodeName, optional, greedy) { }` and the `CommandTree` form |
| `AdventureCompoundBinaryTagArgument(nodeName)` | `SafeOverrideableArgument<CompoundBinaryTag, CompoundBinaryTag>`, `CommandAPIArgumentType.NBT_COMPOUND`, parsed through `SurfPaperNmsCommandArgumentTypesBridge` (`@NmsUseWithCaution`). DSL on `CommandAPICommand`, `CommandTree` and `Argument<*>` |
| `AsyncSignedMessageArgument(nodeName)` | `Argument<Deferred<SignedMessage>>`, `CommandAPIArgumentType.CHAT`, NMS-backed. Read it as `args.awaiting<SignedMessage>(nodeName)` inside a suspending executor — that is the whole point of the `Deferred` |
| `SuspendCustomArgument<T, B>(base, scope: CoroutineScope? = null)` | abstract `CustomArgument<Deferred<T>, B>`. Override `suspend fun CoroutineScope.parse(info: CustomArgumentInfo<B>): T`; `parseArgument` returns `scope.async { … }` and turns a `CustomArgumentException` into a Brigadier exception. `null` means the declaring plugin's scope on the server thread, as for the suspending executors |

The documented usage of `AsyncSignedMessageArgument`, from its own KDoc:

```kotlin
asyncSignedMessageArgument("message")
playerExecutorSuspend { sender, args ->
    val message = args.awaiting<SignedMessage>("message")
    server.sendMessage(message, ChatType.CHAT.bind(sender.displayName()))
}
```

### Suspending executors (`executors/SuspendCommandExecutors.kt`)

`typealias CoroutineScopeProvider = () -> CoroutineScope`. Eight extensions on
`BukkitExecutable<*>`, each wrapping the upstream non-suspending executor of the same name:

```
anyExecutorSuspend  playerExecutorSuspend  entityExecutorSuspend  consoleExecutorSuspend
commandBlockExecutorSuspend  proxyExecutorSuspend  nativeExecutorSuspend  remoteConsoleExecutorSuspend
```

Each takes `scope: CoroutineScopeProvider = callingPluginCommandScope()` and a
`suspend CoroutineScope.(S, CommandArguments) -> Unit`. `callingPluginCommandScope()` is
`@InternalSurfApi`; you only meet it as the default. The default scope is the **declaring
plugin's** coroutine scope with its main dispatcher, so an executor starts on the server thread (on
Folia the global region) and is cancelled with the plugin. The declaring plugin is the one that provides the first class on the stack outside
`dev.slne.surf.api.paper.command.` and outside CommandAPI, determined when the command is declared;
the scope itself is resolved on first execution. No plugin class on the stack fails the
declaration with *"No plugin declares this command: no plugin class is on the stack"*. To run
elsewhere, pass a scope, for example an injected `CoroutineScope`, which starts on
`Dispatchers.Default`:

```kotlin
playerExecutorSuspend(scope = { scope }) { player, args -> … }
```

`launchCommandExecutor` rethrows `CancellationException` and routes everything else to
`handleCommandFailure`, which sends a `WrapperCommandSyntaxException`'s or `CommandSyntaxException`'s
message to the sender (rethrowing when it has none) and otherwise logs
*"Failed to execute command"* and sends `text("Failed to execute command", Colors.ERROR)`.

### Suspending requirements (`requirement/`)

```kotlin
fun <Impl : ExecutableCommand<Impl, CommandSender>> ExecutableCommand<Impl, CommandSender>.withSuspendPlayerRequirement(
    scope: CoroutineScopeProvider = callingPluginCommandScope(),
    allowIfNonPlayer: Boolean = false,
    requirement: suspend CoroutineScope.(Player) -> Boolean,
)
```

The default scope is the same as for the executors. The function delegates to the
`@InternalSurfApi` `SuspendRequirementService`, a bean of surf-api's root context implemented in
`platform/paper/impl/.../command/`. Read the KDoc of `withSuspendPlayerRequirement` before using
it: the suspending predicate is refreshed **asynchronously when the command tree is sent to the
player**, and the *cached* result is what the CommandAPI requirement checks at execution time.
`Player.updateCommands()` forces a refresh. So treat it as command *visibility*, and re-check
anything that must be enforced inside the executor as well.

### Other helpers (`util/`)

`awaitAsyncPlayerProfileOptional(nodeName)` / `awaitAsyncPlayerProfile(nodeName)` await a
`CompletableFuture<List<PlayerProfile>>` argument and return its first profile, unwrapping a
`RuntimeException` to its cause. `PlayerProfile.idOrFail()` and `PlayerProfile.idOrThrow()` for
the UUID live in `dev.slne.surf.api.paper.util` (`player-profile-util.kt`), not in `command/util`.
Inside a command, use `idOrFail()`: a profile without an id fails the command through
`SurfCommandUtil.failWithMessage` with an error message naming the profile. `idOrThrow()` throws
`IllegalStateException` and belongs in non-command code.

## Velocity additions

`platform/velocity/api/src/main/kotlin/dev/slne/surf/api/velocity/command/` — two files.

- `args/MiniMessageArgument(nodeName)`: a plain `Argument<Component>` over
  `StringArgumentType::string` with `CommandAPIArgumentType.PRIMITIVE_TEXT`, parsing with the
  configured `miniMessage` instance and reporting a `ParsingException` through
  `VelocityBrigadierMessage.tooltip(CommandExceptionBuilder(…).build())`. **No `greedy` parameter**
  and only a `CommandAPICommand.miniMessageArgument` DSL function — Paper's version is a
  `CustomArgument` and also has a `CommandTree` overload.
- `args/SuspendCustomArgument<T, B>`: same idea as Paper's but built on
  `Argument<Deferred<T>>(base.nodeName) { base.rawType }`, with its own nested
  `CustomArgumentInfo(sender: CommandSource, previousArgs, input, currentInput: B)` record (note:
  **a different field layout from Paper's and from Minestom's**), a
  `SuspendArgumentSuggestions` inner class for suspending suggestions, and
  `stringCollectionSuspend(…)` to adapt one.
- `executors/SuspendCommandExecutors.kt`: `anyExecutorSuspend`, `playerExecutorSuspend`,
  `consoleExecutorSuspend` on `VelocityExecutable<*>`, each taking
  `scope: CoroutineScope = defaultScope`. There is no plugin-scope extraction here: `defaultScope`
  is a module-level `CoroutineScope(Dispatchers.IO.limitedParallelism(16) + SupervisorJob() +
  CoroutineName("Velocity Default Command Executor Scope") + handler)`. Failure handling mirrors
  Paper's.

Velocity has no entity/command-block/proxy/native executors (Velocity has no such senders), no
suspending requirement service, and no NMS-backed arguments.
