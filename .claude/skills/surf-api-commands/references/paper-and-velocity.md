# Commands on Paper and Velocity

Both platforms use **upstream CommandAPI 12.0.0** — `dev.jorel:commandapi-paper-plugin` /
`commandapi-velocity-core` plus the Kotlin DSL artifacts, declared in `gradle/libs.versions.toml` as
`commandapi = "12.0.0"` (`commandapiplugin = "12.1.0"` for the plugin itself). The builders, the
argument base class, `CommandArguments`, `CustomArgument`, `ArgumentSuggestions` and the
`commandAPICommand` / `commandTree` / `subcommand` DSL all come from `dev.jorel.commandapi` — read
upstream's documentation for those. surf only adds the pieces below.

`platform/paper/api/build.gradle.kts` has `compileOnlyApi(libs.commandapi.paper)` and
`api(libs.commandapi.bukkit.kotlin)`; the Velocity module the equivalent pair.

## Shared, in `common/core/api`

`common/core/api/src/main/kotlin/dev/slne/surf/api/core/command/` — built against
`compileOnlyApi(libs.commandapi.core)`, so it is for Paper and Velocity. **Minestom's module
explicitly excludes `commandapi-core`, so none of this is reachable from Minestom code.**

| File | Holds |
| --- | --- |
| `SurfCommandUtil.kt` | `createException(message: ComponentLike)`, `failWithMessage(message)`, `failWithBuilder(builder)` — all `@JvmStatic` on an `object` |
| `exception/WrapperCommandExceptionComponent.kt` | a `WrapperCommandSyntaxException` whose Brigadier message is a `ComponentMessage`, so a `Component` survives to the client |
| `builder/CommandExceptionBuilder.kt` | formats a parse failure: optional detail message, then `At position <cursor>: ` plus the last `CONTEXT_AMOUNT = 10` characters underlined and a `command.context.here` marker. `build()` uses `Colors.ERROR_PREFIX`; `build(prefix)` takes another |
| `args/SuspendArgumentHelper.kt` | `suspend CommandArguments.awaiting<T>(nodeName)` and `awaitingOrNull<T>(nodeName)` — `await()` a `Deferred<T>` stored as an argument value; `awaiting` errors with *"Argument … is missing"* |

`CommandExceptionBuilder` is the one piece both platforms' `MiniMessageArgument` share.

## Paper additions

`platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/command/`.

### Arguments (`args/`)

| Class | Shape |
| --- | --- |
| `MiniMessageArgument(nodeName, greedy = false)` | `CustomArgument<Component, String>` over a `GreedyStringArgument` or `TextArgument`; a `ParsingException` becomes a `CustomArgumentException.fromAdventureComponent(CommandExceptionBuilder(…).build())`. `greedy` is annotated `@IntroducedAt("3.32.0")`, so the file opts into `ExperimentalVersionOverloading`. DSL: `CommandAPICommand.miniMessageArgument(nodeName, optional, greedy) { }` and the `CommandTree` form |
| `AdventureCompoundBinaryTagArgument(nodeName)` | `SafeOverrideableArgument<CompoundBinaryTag, CompoundBinaryTag>`, `CommandAPIArgumentType.NBT_COMPOUND`, parsed through `SurfPaperNmsCommandArgumentTypesBridge` (`@NmsUseWithCaution`). DSL on `CommandAPICommand`, `CommandTree` and `Argument<*>` |
| `AsyncSignedMessageArgument(nodeName)` | `Argument<Deferred<SignedMessage>>`, `CommandAPIArgumentType.CHAT`, NMS-backed. Read it as `args.awaiting<SignedMessage>(nodeName)` inside a suspending executor — that is the whole point of the `Deferred` |
| `SuspendCustomArgument<T, B>(base, scope = <calling plugin's scope>)` | abstract `CustomArgument<Deferred<T>, B>`. Override `suspend fun CoroutineScope.parse(info: CustomArgumentInfo<B>): T`; `parseArgument` returns `scope.async { … }` and turns a `CustomArgumentException` into a Brigadier exception. The default scope comes from `getCallingPlugin(2)`, which must be a `SuspendingPlugin` |

The documented usage of `AsyncSignedMessageArgument`, from its own KDoc:

```kotlin
asyncSignedMessageArgument("message")
playerExecutorSuspend { sender, args ->
    val message = args.awaiting<SignedMessage>("message")
    server.sendMessage(message, ChatType.CHAT.bind(sender.displayName()))
}
```

### Suspending executors (`executors/SuspendCommandExecutors.kt`)

`typealias CoroutineScopeProvider = () -> CoroutineScope`. Nine extensions on
`BukkitExecutable<*>`, each wrapping the upstream non-suspending executor of the same name:

```
anyExecutorSuspend  playerExecutorSuspend  entityExecutorSuspend  consoleExecutorSuspend
commandBlockExecutorSuspend  proxyExecutorSuspend  nativeExecutorSuspend  remoteConsoleExecutorSuspend
```

Each takes `scope: CoroutineScopeProvider = extractCallingPluginScopeOrThrow()` and a
`suspend CoroutineScope.(S, CommandArguments) -> Unit`.

`extractCallingPluginScopeOrThrow()` walks the stack with `StackWalker`, finds the first frame whose
providing plugin is not the CommandAPI plugin, and requires it to be a
`com.github.shynixn.mccoroutine.folia.SuspendingPlugin` — otherwise
*"Failed to extract CoroutineScope: plugin '…' does not implement SuspendingPlugin."* Pass a scope
explicitly from anything that is not a `SuspendingPlugin`.

`launchCommandExecutor` rethrows `CancellationException` and routes everything else to
`handleCommandFailure`, which sends a `WrapperCommandSyntaxException`'s or `CommandSyntaxException`'s
message to the sender (rethrowing when it has none) and otherwise logs
*"Failed to execute command"* and sends `text("Failed to execute command", Colors.ERROR)`.

### Suspending requirements (`requirement/`)

```kotlin
fun <Impl : ExecutableCommand<Impl, CommandSender>> ExecutableCommand<Impl, CommandSender>.withSuspendPlayerRequirement(
    scope: CoroutineScopeProvider = extractCallingPluginScopeOrThrow(),
    allowIfNonPlayer: Boolean = false,
    requirement: suspend CoroutineScope.(Player) -> Boolean,
)
```

Delegates to the `SuspendRequirementService` (`@InternalSurfApi`, looked up with `requiredService`).
Read its KDoc before using it: the suspending predicate is refreshed **asynchronously when the
command tree is sent to the player**, and the *cached* result is what the CommandAPI requirement
checks at execution time. `Player.updateCommands()` forces a refresh. So treat it as command
*visibility*, and re-check anything that must be enforced inside the executor as well.

### Other helpers (`util/`)

`awaitAsyncPlayerProfileOptional(nodeName)` / `awaitAsyncPlayerProfile(nodeName)` await a
`CompletableFuture<List<PlayerProfile>>` argument and return its first profile, unwrapping a
`RuntimeException` to its cause; `PlayerProfile.idOrThrow()` for the UUID.

## Velocity additions

`platform/velocity/api/src/main/kotlin/dev/slne/surf/api/velocity/command/` — two files.

- `args/MiniMessageArgument(nodeName)`: a plain `Argument<Component>` over
  `StringArgumentType::string` with `CommandAPIArgumentType.PRIMITIVE_TEXT`, parsing with
  `MiniMessage.miniMessage()` directly and reporting a `ParsingException` through
  `VelocityBrigadierMessage.tooltip(CommandExceptionBuilder(…).build())`. **No `greedy` parameter**
  and only a `CommandAPICommand.miniMessageArgument` DSL function — Paper's version is a
  `CustomArgument`, uses the configured `miniMessage` instance and also has a `CommandTree` overload.
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
