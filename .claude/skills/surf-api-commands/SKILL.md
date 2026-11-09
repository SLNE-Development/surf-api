---
name: surf-api-commands
description: Use when working on surf-api commands - choosing between `CommandAPICommand` and `CommandTree`, adding or changing an argument under `command/argument/` or a `*Argument` DSL function, writing a parser or a suggestion provider (`ArgumentSuggestions`, `SafeSuggestions`, `SuggestionsBranch`), reading values out of `CommandArguments` in an executor, picking an executor or a suspending executor, declaring permissions or requirements, registering or unregistering a command on Minestom, Paper or Velocity, or tracking down a `CommandValidationException`, a "no executable paths" or "conflicting arguments" failure or a command that does not appear for a player.
---

# surf-api commands

**Two different command stacks wear the same names.** Know which one you are in before you write a
line.

| | Minestom | Paper and Velocity |
| --- | --- | --- |
| `CommandAPICommand`, `CommandTree`, `Argument`, `CommandArguments` come from | this repository: `platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/command/` (~40 files) | the upstream **CommandAPI 12.0.0** artifacts `dev.jorel:commandapi-*` |
| surf's own code | the whole stack: builders, 40+ argument types, parsers, suggestions, executors, the Brigadier bridge | a thin layer of extra arguments, suspending executors and helpers |
| Package root | `dev.slne.surf.api.minestom.command` | `dev.jorel.commandapi` + `dev.slne.surf.api.paper.command` / `…velocity.command` |
| Registration | `CommandAPI.register` → a Brigadier dispatcher owned by surf | upstream CommandAPI's own registration |

The Minestom files carry the header *"Substantially translated from CommandAPI 12.0.0 … MIT
License, Copyright (c) 2020 - 2022 Jorel Ali"* — so the shape is familiar on purpose, but the types
are not the same types, and `platform/minestom/api/build.gradle.kts` deliberately
`exclude`s `libs.commandapi.core` and `libs.brigadier` from its `surfApiCoreApi` dependency.
`common/core/api/.../command/` (`SurfCommandUtil`, `CommandExceptionBuilder`,
`WrapperCommandExceptionComponent`, `SuspendArgumentHelper`) is built against **upstream**
CommandAPI (`compileOnlyApi(libs.commandapi.core)`) and is therefore for Paper/Velocity, not for
Minestom — the one exception is `CommandExceptionBuilder`, which both Paper's and Velocity's
`MiniMessageArgument` use to format a parse failure.

Everything below is Minestom unless it says otherwise; `references/paper-and-velocity.md` covers the
other two.

## `CommandAPICommand` or `CommandTree`

Both are builders that end in `register(namespace: String? = null)`, both implement
`CommandExecutable`, both carry aliases, permissions, requirements and the four description fields.
The difference is the argument shape:

- **`CommandAPICommand`** — one *linear* argument chain plus subcommands. `withArguments(vararg)`,
  `withOptionalArguments(vararg)` (marks each argument optional), `withSubcommand`/`withSubcommands`.
  Executors run after the last argument of the chain, or on the bare command when there are none.
  Pick it for `/cmd <a> <b>` and for `/cmd sub …` layouts.
- **`CommandTree`** — *branching* arguments. No `withArguments`; `then(argument)` starts a branch,
  and each branch carries its own nested arguments and executors. Permissions and requirements
  declared on the tree apply to every branch. Pick it when the same label takes genuinely different
  argument shapes.

Both flatten into the same internal form at `register()` time: `toDefinition()` → a
`CommandDefinition(name, aliases, metadata, paths)` of `CommandPath(arguments, executors,
permissions, requirements)`. That is where the validation lives, so these failures are
`CommandValidationException` from `register()`, not compile errors:

| Message | Cause |
| --- | --- |
| `Command name/alias '…' must contain only letters, digits, '.', '_' or '-'` | label pattern `[\p{L}\p{N}_.-]+` |
| `Command name and aliases must be unique` | duplicate after lowercasing |
| `Command '…' has no executable paths` | nothing declared an executor |
| `Required arguments cannot follow optional arguments` | per path |
| `Greedy arguments must be terminal` | greedy argument not last |
| `Executable path contains duplicate argument node names` | two non-literal arguments share a `nodeName` |
| `Executable path contains equal-specificity executor conflicts` | two executors of the same `ExecutorType` on one path |
| `Command '…' declares conflicting arguments named '…' at '…'` | sibling paths merge into one node with different `ArgumentKind`s |
| `Command '…' declares more than one executable path ending at '…'` | two paths would execute at the same node |
| `Command '…' contains a subcommand cycle` / `Argument '…' contains a child cycle` | detected by identity, during flattening |

A namespace must match `[a-z0-9_.-]+`; with one, the name and every alias are also registered as
`namespace:label`.

## The Kotlin DSL

`command/dsl/CommandDsl.kt` — `commandAPICommand(name) { }` and `commandTree(name) { }`
**register immediately** (they end in `.register()`); the `(name, namespace) { }` overloads register
namespaced as well. `subcommand(name) { }` builds a `CommandAPICommand` *without* registering, for
`withSubcommand`.

Adapted from `local/minestom-example/src/main/kotlin/dev/slne/surf/api/minestom/example/ExampleCommands.kt`:

```kotlin
commandAPICommand("example") {
    withSubcommand(subcommand("highlight") {
        integerArgument("seconds", min = 1, max = 60, optional = true)
        playerExecutorSuspend { player, args ->
            val seconds = args.getOptional<Int>("seconds") ?: 5
            delay(seconds.seconds)
        }
    })
}

commandAPICommand("gamemode") {
    withAliases("gm")
    withPermission("example.gamemode")
    for (mode in GameMode.entries) {
        withSubcommand(subcommand(mode.name.lowercase()) {
            playerExecutor { player, _ -> player.gameMode = mode }
        })
    }
}
```

`command/dsl/ArgumentDsl.kt` mirrors every argument type three times — as an extension on
`CommandAPICommand` (calls `withArguments`), on `CommandTree` (calls `then`) and on `Argument<T>`
(calls `then`, so it nests). The names are `<kind>Argument(nodeName, …, optional = false) { }`, and
the trailing block configures the created `Argument`. Nesting on an `Argument` is how a tree grows:

```kotlin
commandTree("warp") {
    stringArgument("name") {
        replaceSuggestionsAsync { warpNames() }
        playerExecutor { player, args -> teleport(player, args.get<String>("name")) }
        booleanArgument("silent", optional = true) {
            playerExecutor { player, args -> /* … */ }
        }
    }
}
```

## Arguments

`command/argument/Argument.kt` is the base class. An argument is a mutable builder: `setOptional`
(three overloads: a flag, a constant default, a `(CommandSender) -> T` default), `withPermission`,
`withRequirement`, the four suggestion functions, `addExecutor` (via `CommandExecutable`) and
`then(child)`. It snapshots into an immutable `ArgumentDefinition<T>` carrying `nodeName`,
`kind: ArgumentKind<T>`, `optional`, `defaultValue`, `permissions`, `requirements`,
`suggestions: SuggestionMode<T>`, `greedy`, `rawType: ArgumentType<T>`, `stringify` and
`listDelimiter`.

Groups, by file:

| File | Arguments |
| --- | --- |
| `PrimitiveArguments.kt` | `Boolean`, `Integer`, `Long`, `Float`, `Double` (each with `min`/`max`), `Enum`, `UUID`, `IntegerRange`, `FloatRange` |
| `StringArguments.kt` | `String` (word), `Text` (quotable), `GreedyString`, `Literal`, `MultiLiteral`, `Command` |
| `EntityArguments.kt` | `Player`, `Players`, `Entity`, `Entities` (each `allowEmpty` where plural), `EntityType` |
| `PositionArguments.kt` | `Position`, `Position2D`, `BlockPosition`, `Rotation`, `Angle`, `Axis` (+ the `Axis` enum and `Rotation` data class) |
| `RegistryArguments.kt` | `Sound`, `PotionEffect`, `Biome`, `Enchantment`, `Resource<T>`, `Instance` |
| `MinecraftArguments.kt` | `ResourceLocation`, `Time`, `TeamColor`, `Particle`, `GameMode` |
| `CompositeArguments.kt` | `CustomArgument<T, B>`, `ListArgument<T>`, plus `Argument<T>.map { }` and `Argument<T>.filter { }` |
| `SignedMessageArgument.kt` | `SignedMessage` — greedy, parsed by the platform, not by a Brigadier type |

`command/argument/parser/` holds the Brigadier `ArgumentType` implementations
(`UuidParser`, `TimeParser`, `AngleParser`, `IntegerRangeParser`, `FloatRangeParser`,
`FixedSetParser`, `RegistryParser`, `InstanceParser`, `ParticleParser`, `ResourceLocationParser`,
the coordinate parsers and the 720-line `EntitySelectorParser`). **Every one of them is
`internal`** — they are not API. Use the `Argument` subclass or its DSL function; write a new parser
only when you are adding a new argument type in the same package. Full catalogue, per-argument
behaviour and the `CustomArgument`/`ListArgument` rules: `references/minestom-arguments.md`.

## Reading arguments in an executor

`command/executor/CommandArguments.kt`. It is built from `ParsedArgument(name, value, raw, present)`
entries and offers:

```kotlin
val amount = args.get<Int>("amount")                 // throws when absent, null or the wrong type
val reason = args.getOptional<String>("reason")       // null when absent
val target: Player by args                            // delegate; property name is the node name
args.getRaw("amount"); args.getRaw(0); args[0]        // raw text, raw text by index, value by index
args.rawArguments()                                   // raw text of the present arguments, in order
"reason" in args                                      // declared and present
```

`get` with a non-null `T` throws `IllegalArgumentException` for an unknown name
(*"No argument named '…' exists"*), an absent required argument, a null value or a type mismatch.
Optional arguments with a default resolve to that default; optional arguments without one are
absent, so read them with `getOptional`.

## Executors

`CommandExecutable<SELF>` gives every command, tree and argument `executes`, `executesPlayer` and
`executesConsole`. `command/dsl/ExecutorDsl.kt` adds 18 more extensions, named from three
dimensions — sender (`any` / `player` / `console`), shape (`…Executor` taking `(sender, args)` or
`…ExecutionInfo` taking `ExecutionInfo<S>(sender, args, input)`) and mode (plain, `…Resulting…`
returning the Brigadier `Int`, or `…Suspend`): `anyExecutor`, `playerExecutionInfo`,
`consoleResultingExecutor`, `playerExecutorSuspend`,
`anyExecutionInfoExecutorSuspend`, and so on. There is no suspending *resulting* executor.

The suspending variants take `scope: () -> CoroutineScope = { minestomAsyncScope }` (or a
`CoroutineScope` directly) and are launched by `launchCommandExecutor`, which reports a
`CommandSyntaxException` to the sender as its component and anything else as a logged generic German
failure. Only one executor per `ExecutorType` per path; the bridge prefers the sender-specific one
and falls back to `ANY`. Full matrix: `references/minestom-executors.md`.

## Permissions and requirements

`withPermission(permission: String)` and `withRequirement((CommandSender) -> Boolean)` exist on
`CommandAPICommand`, `CommandTree` and `Argument<T>`. Both accumulate — every permission and every
requirement must pass — and both are inherited down a path by `flatten()`. A blank permission throws
(`CommandValidationException` on a command, `IllegalArgumentException` on an argument).

Resolution goes through `MinestomPermissions` (`minestom/permission/MinestomPermissions.kt`):
`ConsoleSender` and `ServerSender` always resolve to `TriState.TRUE`, a `Player` goes to the
installed `PermissionProvider`, anything else is `NOT_SET` — so with **no provider installed players
hold no permissions at all**, and a non-player, non-console sender passes only while no permission
is declared. Permissions and requirements are also applied when the command tree is declared to a
client, so a sender who fails them does not see the node (`DeclareCommandsMerger`).
A subcommand's own permissions and requirements are copied onto the literal that selects it, which
is what hides the literal rather than only refusing it.

## Registering a command

| Platform | How |
| --- | --- |
| Minestom | `commandAPICommand("x") { … }` / `commandTree("x") { … }`, or `CommandAPICommand("x").…register(namespace)`. `CommandAPI.unregister(name)` removes a label with its aliases and namespaced forms. The platform must be installed (it is, after server startup) or every call throws `IllegalStateException`. A command registered while a `SurfMinestomPlugin` lifecycle function runs, or from within its `scope`, is tracked by `PluginCommands` against that plugin and unregistered when it unloads. A label already used by a native Minestom command is rejected, not shadowed. |
| Paper | upstream CommandAPI through `dev.jorel.commandapi.kotlindsl.commandAPICommand` / `commandTree`; the `commandapi-paper-plugin` artifact is `compileOnlyApi` and provides the runtime |
| Velocity | upstream CommandAPI through `commandapi-velocity-core` + `commandapi-kotlin-velocity` |

`CommandAPI.execute(sender, input)` runs an input (no leading slash) and returns the Brigadier
result; `CommandAPI.failWithString(message)` / `failWithMessage(component)` abort the running
command with a `CommandSyntaxException` that carries a component.

## The Brigadier bridge (Minestom)

`platform/minestom/impl/src/main/kotlin/dev/slne/surf/api/minestom/server/impl/command/brigadier/`.
What it does for you, so that you rarely need to open it:

- builds a real `com.mojang.brigadier.CommandDispatcher` from the flattened `CommandPath`s, so
  parsing, dispatch and `@`-selectors behave like vanilla;
- registers the commands **in that dispatcher, not in Minestom's `CommandManager`** — the manager
  reaches them through `CommandAPIHook`, which `CommandManagerMixin` calls, and falls back to
  Minestom's own handling when no surf platform is active;
- picks the executor matching the sender type at dispatch time and reports
  *"Dieser Befehl kann von diesem Absendertyp nicht ausgeführt werden."* when none fits;
- merges the commands into the `DeclareCommandsPacket` per player, omitting what the player may not
  use, and derives each node's vanilla parser and properties (`NodeDeclaration`) so the client
  validates and suggests like vanilla;
- answers tab completion from the dispatcher (`MinestomSuggestionListener`), running suggestion
  providers in a coroutine so they may suspend;
- renders syntax failures in vanilla's layout — heading, the input with the failing remainder
  underlined, a `command.context.here` marker (`CommandDispatchFailures`);
- supplies the signed message for a `SignedMessageArgument` from the packet, or an unsigned one.

Read `references/minestom-brigadier-bridge.md` before touching it; its internals are only relevant
when you change how arguments are *declared* to the client.

## Not in this skill

Read the sibling skill instead of inferring its rules from here: `surf-api-dialogs` for dialogs,
`surf-api-messages` for `Component` building and `Colors`, `surf-api-inventory` for menus,
`surf-api-components` for the DI container, `surf-api-events` for the event bus, `surf-api-nms` for
the Paper NMS bridges, `surf-api-paper` and `surf-api-minestom` for the plugin/server scaffolding a
command is registered from, and `surf-api-build` for the Gradle side. Paper's
`AdventureCompoundBinaryTagArgument` and `AsyncSignedMessageArgument` go through NMS bridges whose
mechanics belong to `surf-api-nms`.

## References

Read on demand, not up front:

- `references/minestom-arguments.md` — every argument type with its value type, defaults and
  failure messages; `ArgumentKind`; `CustomArgument`, `ListArgument`, `map`, `filter`.
- `references/minestom-executors.md` — the full executor matrix, `ExecutionInfo`,
  `ExecutorDefinition`, the suspending path and failure reporting.
- `references/suggestions.md` — `ArgumentSuggestions`, `SafeSuggestions`, `SuggestionsBranch`,
  `StringTooltip`/`Tooltip`, `SuggestionFilter`, the four suggestion modes and which arguments
  reject them.
- `references/paper-and-velocity.md` — what surf adds on top of upstream CommandAPI: extra
  arguments, suspending executors and requirements, `CommandArguments` await helpers, the shared
  `common/core` exception types.
- `references/minestom-brigadier-bridge.md` — file-by-file walk of the bridge, for when you change
  argument declaration or dispatch.
