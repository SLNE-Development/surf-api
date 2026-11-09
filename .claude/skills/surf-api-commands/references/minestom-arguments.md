# Minestom command arguments

`platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/command/argument/`.

## The base class

`Argument<T>(val nodeName: String) : CommandExecutable<Argument<T>>` — abstract, mutable, not
thread-safe while being configured, every `with*`/`set*` returns `this`. A blank `nodeName` throws in
`init`.

Subclasses override `kind: ArgumentKind<T>`, `rawType: ArgumentType<T>` (the Brigadier parser),
`stringify(value): String` and optionally `greedy` and `listDelimiter`.

Configuration available on every argument:

| Function | Effect |
| --- | --- |
| `setOptional(optional: Boolean)` | flag only; turning it off clears a default |
| `setOptional(default: T)` | optional with a constant default |
| `setOptional(default: (CommandSender) -> T)` | optional with a sender-derived default |
| `withPermission(permission)` | accumulates; blank throws `IllegalArgumentException`; applies to this argument and everything nested below |
| `withRequirement((CommandSender) -> Boolean)` | accumulates; all must pass |
| `replaceSuggestions` / `includeSuggestions` / `replaceSafeSuggestions` / `includeSafeSuggestions` | sets the single `SuggestionMode`; the last call wins |
| `executes`, `executesPlayer`, `executesConsole`, `addExecutor` | the command ends at this argument |
| `then(child: Argument<*>)` | nests; `child === this` throws immediately, longer cycles at registration |

For a `BooleanArgument` the two one-parameter `setOptional` overloads are ambiguous — use the named
form, `setOptional(optional = true)` or `setOptional(default = true)`. The DSL functions take
`optional: Boolean = false` and call `setOptional(optional)`.

`toDefinition(): ArgumentDefinition<T>` is the immutable snapshot the platform compiles:
`nodeName`, `kind`, `optional`, `defaultValue`, `permissions`, `requirements`, `suggestions`,
`greedy`, `rawType`, `stringify`, `listDelimiter`.

`ArgumentKind<T>` is a sealed interface with one case per argument type; it is protocol metadata for
the platform, *not* something to implement outside the package. `UnsupportedArgumentType<T>` is the
placeholder `rawType` for the two kinds the platform parses itself (`Literal`, `SignedMessage`) —
parsing through it always fails.

## The catalogue

### `PrimitiveArguments.kt`

| Class | Value | Notes |
| --- | --- | --- |
| `BooleanArgument(nodeName)` | `Boolean` | `true`/`false` |
| `IntegerArgument(nodeName, min = Int.MIN_VALUE, max = Int.MAX_VALUE)` | `Int` | `min > max` throws |
| `LongArgument(nodeName, min, max)` | `Long` | same |
| `FloatArgument(nodeName, min = -Float.MAX_VALUE, max = Float.MAX_VALUE)` | `Float` | NaN bounds rejected |
| `DoubleArgument(nodeName, min, max)` | `Double` | same |
| `EnumArgument(nodeName, values, formatter = { it.name.lowercase() })` | `E : Enum<E>` | copies into an `EnumSet`; formatted values must be non-blank, unique and unquoted-safe; parsing case-**sensitive**, suggestions case-insensitive |
| `UUIDArgument(nodeName)` | `UUID` | dashed hex |
| `IntegerRangeArgument(nodeName)` | `net.minestom.server.utils.Range.Int` | vanilla `min..max`; omitted bound = the type's extreme; a bare value = a one-value range |
| `FloatRangeArgument(nodeName)` | `Range.Float` | same |

Float and double values are stringified in plain decimal (`toPlainDecimal`), because Brigadier's
reader rejects the exponent notation `toString()` produces for extreme values.

### `StringArguments.kt`

| Class | Value | Notes |
| --- | --- | --- |
| `StringArgument(nodeName)` | `String` | `StringArgumentType.word()` — letters, digits, `_ - . +` |
| `TextArgument(nodeName)` | `String` | `StringArgumentType.string()` — a word or a quoted phrase |
| `GreedyStringArgument(nodeName)` | `String` | greedy; must be last on its path |
| `LiteralArgument(nodeName, literal = nodeName)` | `String` | matched by the tree, **cannot carry a suggestion provider**; blank or whitespace-containing literal throws |
| `MultiLiteralArgument(nodeName, vararg literals)` | `String` | the matched keyword; literals must be non-empty, non-blank, unique and unquoted-safe; parsing case-sensitive |
| `CommandArgument(nodeName)` | `String` | the rest of the line, declared to the client as a command argument; greedy |

### `EntityArguments.kt`

All five parse vanilla selectors and **need a command source** — they cannot be parsed without one.

| Class | Value | Fails when |
| --- | --- | --- |
| `PlayerArgument(nodeName)` | `Player` | the selector can match more than one player (`@a`), or matched none → `No entity matched the selector` |
| `PlayersArgument(nodeName, allowEmpty = false)` | `List<Player>` | the selector can match non-players (`@e`); empty match unless `allowEmpty` → `No entities matched the selector` |
| `EntityArgument(nodeName)` | `Entity` | multi-match selector; no match |
| `EntitiesArgument(nodeName, allowEmpty = false)` | `List<Entity>` | empty match unless `allowEmpty` |
| `EntityTypeArgument(nodeName)` | `EntityType` | unknown key; namespace defaults to `minecraft`. Rejects the *including* suggestion modes — see below |

### `PositionArguments.kt`

`enum class Axis { X, Y, Z }` and `data class Rotation(val yaw: Float, val pitch: Float)` live here.

| Class | Value | Notes |
| --- | --- | --- |
| `PositionArgument` | `Vec` | three coordinates, absolute, `~`-relative or `^`-local, already resolved against the sender |
| `Position2DArgument` | `Vec` | `x` and `z`, `y = 0`; no local coordinates |
| `BlockPositionArgument` | `Vec` | whole numbers or `~`; relative offsets may be decimal, so the result is not guaranteed block-aligned |
| `RotationArgument` | `Rotation` | yaw then pitch, resolved against the sender's view |
| `AngleArgument` | `Float` | degrees, wrapped into `[-180, 180)` |
| `AxisArgument` | `Set<Axis>` | a swizzle such as `xz`, case-insensitive, order preserved; repeats rejected |

`PositionArgument`, `Position2DArgument`, `BlockPositionArgument` and `RotationArgument`
**cannot carry a suggestion provider** — see "Which arguments reject suggestions" below.

### `RegistryArguments.kt`

| Class | Value |
| --- | --- |
| `SoundArgument` | `SoundEvent` |
| `PotionEffectArgument` | `PotionEffect` |
| `BiomeArgument` | `RegistryKey<Biome>`, looked up in the server's biome registry |
| `EnchantmentArgument` | `RegistryKey<Enchantment>`, same for enchantments |
| `ResourceArgument<T>(nodeName, identifier, registry)` | `RegistryKey<T>` — `identifier` is sent to the client and appears in the `Unknown <identifier>` failure |
| `InstanceArgument` | `Instance`, by UUID or by dimension name (a name resolves only while exactly one instance uses it) |

### `MinecraftArguments.kt`

| Class | Value | Notes |
| --- | --- | --- |
| `ResourceLocationArgument` | `Key` | namespace defaults to `minecraft` |
| `TimeArgument` | `java.time.Duration` | vanilla syntax, unit `d`/`s`/`t`, no unit = ticks; negatives rejected |
| `TeamColorArgument` | `TeamColor` | by `toString()`, parsing case-sensitive |
| `ParticleArgument` | `Particle` | **only particles without options**; `dust`, `block`, `vibration` and friends are rejected as unsupported |
| `GameModeArgument` | `GameMode` | lowercase name |

### `SignedMessageArgument.kt`

`SignedMessageArgument(nodeName)` → `net.kyori.adventure.chat.SignedMessage`, greedy, must be last.
Its `rawType` is an `UnsupportedArgumentType`: the platform supplies the signed message the client
sent for this node, or an unsigned one built from the text. It cannot be the base of a
`CustomArgument` or the element of a `ListArgument`.

### `CompositeArguments.kt`

```kotlin
data class CustomArgumentInfo<B>(val sender: CommandSender, val currentInput: String, val baseValue: B)

open class CustomArgument<T, B>(base: Argument<B>, parser: (CustomArgumentInfo<B>) -> T) : Argument<T>(base.nodeName)
class ListArgument<T>(nodeName: String, element: Argument<T>, delimiter: Char = ',', allowEmpty: Boolean = false) : Argument<List<T>>(nodeName)

fun <T, R> Argument<T>.map(mapper: (T) -> R): Argument<R>
fun <T> Argument<T>.filter(predicate: (T) -> Boolean): Argument<T>
```

`CustomArgument` keeps the base's `nodeName`, so the value is read under the same name. It inherits
the base's optional flag, default value, permissions and requirements — a default is passed through
the parser with an empty `currentInput`. Suggestions come from the base's raw type; a **suggestion
provider set on the base is not inherited**. Values are stringified with `toString()` unless the
internal `formatter` constructor is used, which is what `filter` does to keep the base's formatter.
The parser reports a bad value by throwing a Brigadier `CommandSyntaxException`, surf's own
`CommandSyntaxException` (for example via `CommandAPI.failWithMessage`) or a
`WrapperCommandSyntaxException`; the failure is positioned at the start of the argument. Parsing
needs a command source.

`ListArgument` is greedy and must be last. Whitespace around the delimiter and around the whole list
is skipped, so a list may span several space-separated tokens. The delimiter must be a visible
non-whitespace character, the element must not be greedy, and the element's permissions and
requirements carry over. Without `allowEmpty`, an empty list and an empty element (leading, trailing
or doubled delimiter) fail with `List must not be empty`. Suggestions are offered for the element
after the last delimiter.

`map` stringifies with `toString()`, which matters for safe suggestions. `filter` fails a rejected
value with `Invalid value for <nodeName>`.

Both reject a `LiteralArgument` and a `SignedMessageArgument` as base/element with
*"Argument '…' cannot use a literal argument as its base"* (or `… as its element`).

## The DSL names

`command/dsl/ArgumentDsl.kt` exposes each type three times — on `CommandAPICommand` (appends to the
linear chain), on `CommandTree` (`then`, a new branch) and on `Argument<T>` (`then`, nested). The
names:

```
booleanArgument  integerArgument  longArgument  floatArgument  doubleArgument
stringArgument  textArgument  greedyStringArgument  literalArgument  multiLiteralArgument
enumArgument  uuidArgument  integerRangeArgument  floatRangeArgument
playerArgument  playersArgument  entityArgument  entitiesArgument  entityTypeArgument
locationArgument  location2DArgument  blockPositionArgument  rotationArgument  angleArgument  axisArgument
resourceLocationArgument  timeArgument  teamColorArgument  particleArgument  gameModeArgument
soundArgument  potionEffectArgument  biomeArgument  enchantmentArgument  resourceArgument  instanceArgument
commandArgument  signedMessageArgument  customArgument  listArgument
```

Note `locationArgument` / `location2DArgument` for `PositionArgument` / `Position2DArgument` — the
DSL name does not match the class name. `literalArgument` and `multiLiteralArgument` take no
`optional` parameter. `enumArgument(nodeName, values, formatter, optional) { }` takes a `noinline`
formatter. `customArgument(base, parser, optional: Boolean? = null) { }` leaves the base's
optionality alone when `optional` is `null`.

## Which arguments reject suggestions

`MinestomCommandCompiler.validateSuggestionModes` (in `platform/minestom/impl/.../command/`) runs at
registration and throws `CommandValidationException`:

- `Literal`, `Command`, `Position`, `Position2D`, `BlockPosition` and `Rotation` reject **any**
  non-built-in mode: *"<Kind> argument '<nodeName>' cannot use custom suggestions"*.
- `EntityType` rejects only `Include` and `IncludeSafe`, because Minestom does not expose the native
  summonable entity set to merge into — replacing is allowed.

The check resolves through wrappers: `ArgumentKind.baseKind()` unwraps `Custom` to its base and
`List` to its element, so wrapping a position in a `CustomArgument` does not get you a suggestion
provider.

## Parsers

`command/argument/parser/` holds the `ArgumentType` implementations behind all of this. Every class
and object in that package is `internal`: `UuidParser`, `AngleParser`, `TimeParser`,
`IntegerRangeParser`, `FloatRangeParser`, `FixedSetParser`, `RegistryParser`, `InstanceParser`,
`ParticleParser`, `ResourceLocationParser`, `PositionParser`, `RotationParser`, `AxisParser`,
`PositionArgumentType`, `RotationArgumentType`, `EntitySelector`, `EntitySelectorParser`, and the
`suggestMatching` helper in `Suggesting.kt`. Add a parser there only together with the `Argument`
subclass and `ArgumentKind` case that use it, and with the matching `NodeDeclaration` entry in the
Brigadier bridge.
