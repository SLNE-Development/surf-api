# Minestom argument suggestions

`platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/command/suggestion/` plus the
extensions in `command/dsl/SuggestionDsl.kt`.

## The four modes

`SuggestionMode<out T>` in `command/argument/Argument.kt`:

| Mode | Set by | Behaviour |
| --- | --- | --- |
| `BuiltIns` | the default | only what the argument's own parser suggests |
| `Include<T>(provider)` | `includeSuggestions` | the provider's entries **plus** the built-ins |
| `Replace<T>(provider)` | `replaceSuggestions` | only the provider's entries |
| `IncludeSafe<T>(provider)` | `includeSafeSuggestions` | typed values, converted with the argument's `stringify`, plus the built-ins |
| `ReplaceSafe<T>(provider)` | `replaceSafeSuggestions` | typed values only |

An argument holds exactly one mode; the last call wins. Which arguments refuse a mode is listed in
`references/minestom-arguments.md` ("Which arguments reject suggestions") — short version: literal,
command and the three position kinds plus rotation refuse everything, `EntityType` refuses the two
`Include*` modes.

## `ArgumentSuggestions`

```kotlin
fun interface ArgumentSuggestions {
    suspend fun suggest(info: SuggestionInfo): List<StringTooltip>
    val filterPolicy: SuggestionFilter get() = SuggestionFilter.PREFIX
}
```

Providers run **in a coroutine on the server's asynchronous scope**, not on the network thread, and
may suspend. A provider that throws is logged (*"Suggestion provider failed and contributes no
entries"*) and contributes nothing; cancellation propagates.

Factories on the companion:

| Factory | Returns entries from |
| --- | --- |
| `empty()` | nothing |
| `strings(vararg String)` / `strings(Collection<String>)` | a fixed list, no tooltips |
| `strings(provider: (SuggestionInfo) -> Collection<String>)` | a blocking provider, per request |
| `stringsAsync(provider: suspend (SuggestionInfo) -> Collection<String>)` | a suspending provider |
| `stringsWithTooltips(…)` / `stringsWithTooltipsAsync(…)` | the same three shapes, with `StringTooltip` entries |
| `unfiltered(provider)` | a suspending provider whose `filterPolicy` is `SuggestionFilter.NONE` |
| `merge(vararg providers)` | all providers, run **concurrently** via `async`/`awaitAll`, concatenated; the merged provider filters by prefix and does not combine the inputs' filter policies; one failing provider is logged and contributes nothing |
| `branch(branch: SuggestionsBranch)` | the branch's provider for the current position |

`stringCollection`, `stringCollectionAsync`, `stringsWithTooltipsCollection` and
`stringsWithTooltipsCollectionAsync` are aliases of the provider overloads, for Java call sites where
the overload would be ambiguous.

## `SafeSuggestions<T>`

```kotlin
fun interface SafeSuggestions<T> { suspend fun suggest(info: SuggestionInfo): List<Tooltip<T>> }
```

Suggests values of the argument's own type; the platform converts each with the argument's
`stringify`, so every suggestion is spelled the way the argument parses it. Entries are **always**
filtered by the typed prefix — there is no `unfiltered` equivalent. Factories: `empty()`,
`suggest(vararg T)`, `suggest(Collection<T>)`, `suggest(provider)`, `suggestAsync(provider)`,
`tooltips(…)`, `tooltipsAsync(…)`, plus the `…Collection`/`…CollectionAsync` aliases.

## Supporting types (`Tooltip.kt`)

```kotlin
data class Tooltip<T>(val suggestion: T, val tooltip: Component?)
data class StringTooltip(val suggestion: String, val tooltip: Component?) {
    companion object { fun ofString(suggestion: String, tooltip: Component? = null): StringTooltip }
}
data class SuggestionInfo(
    val sender: CommandSender,
    val previousArgs: CommandArguments,
    val currentInput: String,
    val currentArg: String,
)
enum class SuggestionFilter { PREFIX, NONE }
```

## The DSL shorthands

`command/dsl/SuggestionDsl.kt` wraps the factories so you rarely name them. For every combination of
`replace`/`include` × plain/`WithTooltips`/safe/`SafeWithTooltips` × fixed-values/provider/async
there is an extension on `Argument<T>`:

```kotlin
stringArgument("warp") {
    replaceSuggestions("spawn", "shop")                       // fixed strings
    replaceSuggestionsAsync { info -> loadWarpNames(info) }    // suspending provider
    includeSuggestionsWithTooltips(StringTooltip.ofString("spawn", text("the spawn")))
}
```

Full name list: `replaceSuggestions`, `includeSuggestions`, `replaceSuggestionsAsync`,
`includeSuggestionsAsync`, `replaceSuggestionsWithTooltips`, `includeSuggestionsWithTooltips`,
`replaceSuggestionsWithTooltipsAsync`, `includeSuggestionsWithTooltipsAsync`,
`replaceSafeSuggestions`, `includeSafeSuggestions`, `replaceSafeSuggestionsAsync`,
`includeSafeSuggestionsAsync`, `replaceSafeSuggestionsWithTooltips`,
`includeSafeSuggestionsWithTooltips`, `replaceSafeSuggestionsWithTooltipsAsync`,
`includeSafeSuggestionsWithTooltipsAsync`, plus `replaceSuggestions(branch)` /
`includeSuggestions(branch)`.

**Overload trap, documented in the file itself:** a lambda literal passed straight to
`replaceSuggestions { … }` resolves to the *member* `Argument.replaceSuggestions(ArgumentSuggestions)`
— the SAM-converted interface — not to the extension taking
`(SuggestionInfo) -> Collection<String>`. Since `ArgumentSuggestions.suggest` is `suspend` and
returns `List<StringTooltip>`, a lambda returning strings will not compile against it. Either use
the `…Async` extension, or give the value an explicit
`(SuggestionInfo) -> Collection<String>` type. The same applies to the safe variants.

## `SuggestionsBranch`

For a sequence of arguments where the valid values at one position depend on what was typed before.

```kotlin
val branch = SuggestionsBranch.suggest(
    ArgumentSuggestions.strings("add", "remove"),
    null,                                           // any value accepted here
).branch(
    SuggestionsBranch.suggest(ArgumentSuggestions.strings("yes", "no")),
)
```

- `SuggestionsBranch.suggest(vararg ArgumentSuggestions?)` declares one provider per position, in
  order; `null` marks a position that accepts any value and offers nothing.
- `branch(vararg SuggestionsBranch)` adds alternative continuations **after** this branch's own
  positions.
- `getNextSuggestion(sender, vararg previousRaw): ArgumentSuggestions?` returns the provider for the
  position after the given raw values, or `null`.
- `enforceReplacements(sender, vararg previousRaw)` validates a complete sequence instead.

A value typed at a position that has a provider must equal one of that provider's suggestions;
otherwise traversal throws surf's `CommandSyntaxException` with `Unknown command` (at position 0) or
`Unknown argument`, carrying the joined input and a cursor. When several continuations match, their
providers are merged with `ArgumentSuggestions.merge`; when all fail, the failure with the largest
cursor is rethrown.

`ArgumentSuggestions.branch(branch)` adapts it to a normal provider: it calls `getNextSuggestion`
with `info.previousArgs.rawArguments()`, swallows a `CommandSyntaxException` into no entries, and
offers whatever the selected provider returns. The same branch object is therefore attached to
*every* argument it covers — it selects the right position from how many raw values precede it.

Note the `SuggestionInfo` that a branch builds for its own provider calls is synthetic: the parsed
arguments are named `"0"`, `"1"`, … and `currentArg` is `""`. A provider meant for use inside a
branch must not rely on `previousArgs.get<T>("name")`.
