# Minestom executors and `CommandArguments`

`platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/command/executor/` and
`command/dsl/ExecutorDsl.kt`.

## The model

```kotlin
enum class ExecutorType { ANY, PLAYER, CONSOLE }

data class ExecutionInfo<S : CommandSender>(val sender: S, val args: CommandArguments, val input: String)

sealed interface ExecutorDefinition {
    val type: ExecutorType
    data class Normal(override val type: ExecutorType, val executor: (ExecutionInfo<CommandSender>) -> Unit) : ExecutorDefinition
    data class Resulting(override val type: ExecutorType, val executor: (ExecutionInfo<CommandSender>) -> Int) : ExecutorDefinition
    data class Suspending(
        override val type: ExecutorType,
        val scopeProvider: () -> CoroutineScope,
        val executor: suspend CoroutineScope.(ExecutionInfo<CommandSender>) -> Unit,
    ) : ExecutorDefinition
}
```

`input` is the raw command line the dispatch started from. A `Resulting` executor's `Int` is the
Brigadier result that `CommandAPI.execute` returns.

`CommandExecutable<SELF>` is implemented by `CommandAPICommand`, `CommandTree` and `Argument<T>`.
It declares `addExecutor(definition): SELF` (`@InternalSurfApi`) and three convenience members:

```kotlin
fun executes(executor: (CommandSender, CommandArguments) -> Unit): SELF
fun executesPlayer(executor: (Player, CommandArguments) -> Unit): SELF
fun executesConsole(executor: (ConsoleSender, CommandArguments) -> Unit): SELF
```

The player and console members `check` the runtime type and fail loudly
(*"PLAYER executor received …"*) if the dispatcher ever picks the wrong one.

**One executor per `ExecutorType` per path.** A second `ANY` executor on the same path fails
registration with *"Executable path contains equal-specificity executor conflicts"*. An `ANY` plus a
`PLAYER` executor is fine — the bridge prefers the specific one and falls back to `ANY`.

## The DSL matrix

`command/dsl/ExecutorDsl.kt`, all extensions on `SELF : CommandExecutable<SELF>`:

| | `(sender, args)` | `ExecutionInfo<S>` |
| --- | --- | --- |
| `Unit`, any | `anyExecutor` | `anyExecutionInfo` |
| `Unit`, player | `playerExecutor` | `playerExecutionInfo` |
| `Unit`, console | `consoleExecutor` | `consoleExecutionInfo` |
| `Int`, any | `anyResultingExecutor` | `anyResultingExecutionInfo` |
| `Int`, player | `playerResultingExecutor` | `playerResultingExecutionInfo` |
| `Int`, console | `consoleResultingExecutor` | `consoleResultingExecutionInfo` |
| `suspend`, any | `anyExecutorSuspend` | `anyExecutionInfoExecutorSuspend` |
| `suspend`, player | `playerExecutorSuspend` | `playerExecutionInfoExecutorSuspend` |
| `suspend`, console | `consoleExecutorSuspend` | `consoleExecutionInfoExecutorSuspend` |

The `ExecutionInfo` variants hand the player/console executor an `ExecutionInfo<Player>` /
`ExecutionInfo<ConsoleSender>`, narrowed from the generic one. There is no suspending *resulting*
executor.

Every suspending function comes in two overloads:

```kotlin
fun <SELF : CommandExecutable<SELF>> SELF.playerExecutorSuspend(
    scope: () -> CoroutineScope = { minestomAsyncScope },
    executor: suspend CoroutineScope.(Player, CommandArguments) -> Unit,
): SELF

fun <SELF : CommandExecutable<SELF>> SELF.playerExecutorSuspend(
    scope: CoroutineScope,
    executor: suspend CoroutineScope.(Player, CommandArguments) -> Unit,
): SELF
```

The default scope is `minestomAsyncScope` from
`minestom/coroutine/MinestomScopeProvider.kt` — the server's asynchronous scope, **not** the tick
thread. Work that must run on a tick has to be dispatched there explicitly.

## How a suspending executor fails

`launchCommandExecutor(definition, sender, arguments, input)` in `executor/SuspendExecutors.kt`
launches in `definition.scopeProvider()` and wraps the body:

- surf's `CommandSyntaxException` or a `WrapperCommandSyntaxException` → its `component` is sent to
  the sender; if the component is `null` the exception is rethrown;
- a `CancellationException` that is not a `TimeoutCancellationException` → rethrown, so coroutine
  cancellation still works;
- a `TimeoutCancellationException` from the executor's own `withTimeout` → reported like any other
  failure;
- anything else → logged via `MinecraftServer.LOGGER` as
  `Failed to execute command '{}' for {}` and answered with `GENERIC_COMMAND_FAILURE`:
  *"Beim Ausführen des Befehls ist ein Fehler aufgetreten."* (red, hard-coded German).

`handleCommandExecutorFailure(sender, input, failure)` is the same logic exposed for the synchronous
path, which the Brigadier bridge calls.

## `CommandArguments`

`executor/CommandArguments.kt`. Built by the platform from
`ParsedArgument(name, value: Any?, raw: String?, present: Boolean)` entries — `of(list)` requires the
names to be unique; `empty()` for none.

```kotlin
inline fun <reified T> get(name: String): T
inline fun <reified T> getOptional(name: String): T?
fun getRaw(name: String): String?
fun getRaw(index: Int): String?
fun rawArguments(): List<String>
operator fun get(index: Int): Any?
operator fun contains(name: String): Boolean
inline operator fun <reified T> getValue(thisRef: Any?, property: KProperty<*>): T
```

- `get<T>` resolves through `typed(name, T::class, nullable = null is T)`, so `get<Int?>("x")` is
  allowed to return `null` while `get<Int>("x")` is not.
- Failures are all `IllegalArgumentException`: *"No argument named '…' exists"*,
  *"Required argument '…' is absent"*, *"Argument '…' is null but … was required"*,
  *"Argument '…' expected …, got …"*.
- An absent optional argument **with** a default resolves to that default (the platform fills the
  `ParsedArgument`); without one it is absent, so use `getOptional`.
- `rawArguments()` returns the raw text of the *present* arguments only, in declaration order — this
  is what `SuggestionsBranch` matches against.
- The delegate uses the property name as the node name: `val target: Player by args`.

Example, as `local/minestom-example/.../ExampleCommands.kt` writes it:

```kotlin
integerArgument("seconds", min = 1, max = 60, optional = true)
playerExecutorSuspend { player, args ->
    val seconds = args.getOptional<Int>("seconds") ?: 5
    delay(seconds.seconds)
}
```
