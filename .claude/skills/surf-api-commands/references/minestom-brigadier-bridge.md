# The Minestom Brigadier bridge

`platform/minestom/impl/src/main/kotlin/dev/slne/surf/api/minestom/server/impl/command/`. The api
module declares commands; this module turns a `CommandDefinition` into something a client and a
dispatcher understand. Read it when you add an `ArgumentKind`, change how an argument is declared to
the client, or chase a command that parses on the server but is rejected or mis-suggested by the
client.

## What it gives you

- A real `com.mojang.brigadier.CommandDispatcher<CommandSender>`, so parsing, dispatch, selectors
  and syntax failures behave like vanilla.
- Commands live **in that dispatcher, not in Minestom's `CommandManager`**. The manager reaches them
  through `CommandAPIHook`, which `CommandManagerMixin` calls — so **the server must run with the
  surf Mixin agent**. With no platform active every hook returns `null`/`false` and Minestom handles
  the input itself.
- Sender-specific executor selection, with a report when nothing fits.
- A per-player `DeclareCommandsPacket` that hides what the player may not use.
- Tab completion answered from the dispatcher, with suspending suggestion providers.
- Vanilla-shaped syntax failure messages.
- The signed message for a `SignedMessageArgument`.

## File by file

| File | Role |
| --- | --- |
| `MinestomCommandAPIService.kt` | lifecycle. `start(node)` registers `CommandAPITranslations`, sets `CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES = false` process-wide, installs the platform, registers the tab-completion listener and replaces the manager's unknown-command callback; `stop()` undoes it. Not thread-safe; belongs to the server lifecycle |
| `MinestomCommandAPIPlatform.kt` | the `CommandAPIPlatform` implementation: `register`, `unregister`, `execute`. Guards state with a `ReentrantLock`, rolls a failed registration back completely, tracks the active platform in a companion `AtomicReference`. Queries `CommandManager` at registration so a label already used by a native Minestom command is **rejected, not shadowed** |
| `MinestomCommandOwnership.kt` | the label → registration map. `contains`, `find`, `findInput(text)` (strips a leading `/` and takes the first word), `claim` (fails with *"Command name '…' is already owned"*) |
| `MinestomCommandCompiler.kt` | derives the lowercased labels a definition occupies (name, aliases, and `namespace:label` for each) and runs `validateSuggestionModes` |
| `CommandAPIHook.kt` | the `@JvmStatic` entry points the mixin calls: `owns(name)`, `execute(sender, input)`, and the declare-commands merge. A syntax failure is reported in vanilla's layout and yields `CommandResult.Type.INVALID_SYNTAX`; any other `Throwable` is logged and answered with the generic failure, yielding `CANCELLED` |
| `MinestomSuggestionListener.kt` | cancels a `ClientTabCompletePacket` whose first word is an owned label and answers from the dispatcher when the suggestion future completes, on whichever thread completes it. A failed future answers empty. At most 1000 entries |
| `MinestomConditions.kt` | `canUse(sender, permissions, requirements)` and `commonForPaths(paths)` — the permissions and requirements *every* path shares, used for the node-level check |
| `CommandAPITranslations.kt` | English fallbacks for `command.unknown.command`, `command.unknown.argument`, `command.context.here`, registered once in `GlobalTranslator` for senders rendered server-side (the console); players receive the keys and render them themselves |
| `SignedCommandArguments.kt` | a `ThreadLocal<Map<String, SignedMessage>>` holding the signed messages of the command being dispatched, keyed by node name, plus a replaceable `unsignedFactory: (UUID?, String) -> SignedMessage` (default `SignedMessage.system(content, null)`) that the signed-chat feature installs |
| `ArgumentSupport.kt` | `SuggestionMode.includesBuiltIns()` and the `tailrec ArgumentKind.baseKind()` that unwraps `Custom` and `List` |
| `PluginCommands.kt` | tracks which `SurfMinestomPlugin` registered which label; `unregisterAll(plugin)` on unload |
| `brigadier/BrigadierCommandTree.kt` | builds the dispatcher |
| `brigadier/NodeDeclaration.kt` | what the client is told about a node |
| `brigadier/DeclareCommandsMerger.kt` | merges the commands into the player's command packet |
| `brigadier/SuggestingArgumentType.kt` | serves a suggestion provider |
| `brigadier/SignedMessageArgumentType.kt` | the parser for a `SignedMessageArgument` |
| `brigadier/CommandDispatchFailures.kt` | vanilla-shaped failure reports |

## `BrigadierCommandTree`

Nodes are built at **registration** time, so a definition the bridge cannot build fails there. The
dispatcher itself is rebuilt lazily on the first read after the command set changed, because
Brigadier cannot detach a node. `register`/`unregister` must be serialised by the caller (the
platform's lock does that); `dispatcher()` and `registered()` are safe from any thread.

Two details worth knowing when you read it:

- `CommandPath.executableDepths()` is the single source of truth for *where* a path can execute —
  its full length, or every length from the first optional argument onwards. The Brigadier tree and
  the declared tree both derive their executable nodes from it and have to agree, which is also why
  `validateAndFreezePaths` in the api module rejects paths that would merge with a conflict.
- `selectExecutor` picks the executor whose `ExecutorType` matches the sender, preferring
  `PLAYER`/`CONSOLE` over `ANY`, and reports
  *"Dieser Befehl kann von diesem Absendertyp nicht ausgeführt werden."* when none matches. A
  `Suspending` executor is launched through `launchCommandExecutor`; a `Resulting` one's `Int` is
  returned to Brigadier. A sender that is neither a `Player` nor a `ConsoleSender` can only match an
  `ANY` executor.
- `argumentsOf(context, path, depth)` fills the `CommandArguments`: an argument at or beyond `depth`
  is marked absent and takes its declared default value, literals are dropped, and `raw` is the
  exact substring of the input the node consumed.

## `NodeDeclaration` and `DeclareCommandsMerger`

A `NodeDeclaration(parser: ArgumentParserType, properties: ByteArray?, suggestionsType: String?)` is
**protocol data only** — the server still parses with the argument's own Brigadier type. For most
kinds the parser and properties are read from an equivalent native Minestom argument; the rest name
their parser directly. This is the part you extend when you add an `ArgumentKind`: without an entry
here the client will not validate or suggest the new argument correctly, even though the server
parses it.

`DeclareCommandsMerger.merge(original, sender)` appends a root literal per label the sender can use,
omitting paths and arguments whose permissions or requirements the sender fails and dropping a
command with no usable path entirely. A root literal of the original packet that carries one of the
CommandAPI's labels is removed, so the merged tree is the only declaration of that label. It holds no
per-call state and is safe from any thread.

## `SuggestingArgumentType`

Wraps the argument's real `ArgumentType`, delegating `parse` and `getExamples` unchanged, and serves
the declared `SuggestionMode` in `listSuggestions`: it launches a coroutine on the suggestion scope
(`minestomAsyncScope` by default), builds a `SuggestionInfo` from the previously parsed arguments,
runs the provider — which may suspend — converts `SafeSuggestions` values with the argument's
`stringify`, applies `SuggestionFilter` (safe modes are always `PREFIX`) and merges with the
delegate's own suggestions for the `Include*` modes.

The returned future **always completes normally**: a failing provider is logged as
*"Failed suggestions for argument '…'"* and the entries collected so far are still returned. A
source that is not a `CommandSender` yields `Suggestions.empty()`. The results are merged with
`Suggestions.merge` rather than assumed to share one builder, because a list argument offsets a
builder per element.

`previousArguments(context)` builds the `SuggestionInfo.previousArgs` from the nodes parsed so far,
omitting literals and any node whose value cannot be read — so a provider can read an earlier
argument by its node name, which is what distinguishes it from the synthetic info a
`SuggestionsBranch` builds.
