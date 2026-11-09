# Config constraints and helper types

Source: `common/config/api/src/main/kotlin/dev/slne/surf/api/config/constraints/` and
`.../config/type/`.

## Constraints

All of them target a **field**, so in Kotlin they need the `@field:` use-site target on a
constructor property. Every one is wired into the object mapper in
`SpongeConfigSerializers.buildSerializersModule()`, and a violation throws
`SerializationException` while the config is being deserialized - which the manager reports as
`SerializationConfigException`. A `null` value is ignored by every constraint.

```kotlin
@ConfigSerializable
data class ServerConfig(
    @field:NotBlank
    @field:MaxLength(32)
    var serverName: String = "lobby",

    @field:Range(min = 0.0, max = 1.0)
    var chance: Double = 0.5,

    @field:MinSize(1)
    @field:NoDuplicates
    var worlds: List<String> = listOf("world"),
)
```

| Annotation | Parameters | Applies to |
| --- | --- | --- |
| `@PositiveNumber` | - | `Number`, also `IntOr` / `DoubleOr`. Rejects `<= 0` |
| `@NegativeNumber` | - | `Number`, also `IntOr` / `DoubleOr`. Rejects `>= 0` |
| `@MinNumber` | `min: Double` | `Number`, `IntOr`, `DoubleOr` |
| `@MaxNumber` | `max: Double` | `Number`, `IntOr`, `DoubleOr` |
| `@Range` | `min: Double`, `max: Double` | `Number`, `IntOr`, `DoubleOr`. Inclusive |
| `@NotBlank` | - | `String` |
| `@Trimmed` | - | `String`. Validates only, never trims the value |
| `@MinLength` | `min: Int` | `String`, also `StringOrDefault` |
| `@MaxLength` | `max: Int` | `String`, also `StringOrDefault` |
| `@StartsWith` | `prefix: String` | `String`, also `StringOrDefault` |
| `@EndsWith` | `suffix: String` | `String`, also `StringOrDefault` |
| `@Contains` | `value: String` | `String`, also `StringOrDefault` |
| `@DisallowValues` | `vararg values: String` | `String` |
| `@MinSize` | `min: Int` | collection, map, array, string |
| `@MaxSize` | `max: Int` | collection, map, array, string |
| `@NotEmpty` | - | collection, map, array, string |
| `@NoDuplicates` | - | `Iterable<*>`, array |
| `@MinDuration` | `seconds: Long` | `ConfigDuration`, also `DurationOrDisabled` |
| `@MaxDuration` | `seconds: Long` | `ConfigDuration`, also `DurationOrDisabled` |
| `@Namespace` | `namespace: String` | Adventure `Key` |
| `@ExistingFile` | - | `Path`, `File`, `String`; must exist and be a regular file |
| `@Directory` | - | `Path`, `File`, `String`; must exist and be a directory |
| `@WritablePath` | - | `Path`, `File`, `String`; must be writable **if it exists** |

Notes that bite:

- The `…OrDefault` / `…OrDisabled` variants exist because a constraint factory is bound to a value
  type. `@field:Range` on an `IntOr.Default` works; on an unrelated wrapper type it is simply never
  applied - no error, no validation.
- `@MinDuration` / `@MaxDuration` take **seconds**, not a duration string.
- The path constraints accept three input types and silently skip a value they cannot turn into a
  `Path`.
- Configurate's own `@Required` and `@Matches` also work, since the manager keeps the default
  constraint set.

## Helper types

In `config/type/`. All of them serialize as a single scalar, so a user can write either a value or
a keyword.

### `ConfigDuration`

A duration written as one number plus one unit: `d`, `h`, `m`, `s`, case-insensitive, optional
decimals, e.g. `30s`, `1.5h`, `2d`. Parsing is a single-unit regex - `1h30m` does **not** parse.

```kotlin
@ConfigSerializable
data class TimeoutConfig(
    @field:MaxDuration(seconds = 300)
    var timeout: ConfigDuration = ConfigDuration(30.seconds),
)

val d: Duration = config.timeout.asDuration()
val secs: Long = config.timeout.inWholeSeconds()
```

`ConfigDuration(value: Duration, rawValue: String = format(value))` keeps the raw text so the file
round-trips unchanged. `format` throws on an infinite duration.

### `DurationOrDisabled`

A duration or the word `disabled` (case-insensitive).

```kotlin
var timeout: DurationOrDisabled = DurationOrDisabled.DISABLED

val effective = config.timeout or 30.seconds
if (config.timeout.isDisabled()) { … }
```

Only `DISABLED` is constructible from code: the data class has a `private constructor` and is
`@ConsistentCopyVisibility`, and the companion exposes no factory. A present duration can therefore
come only from the file. Use `ConfigDuration` for a field you also want to set programmatically.

### `BooleanOrDefault`

`true`, `false` or `default`. Constants: `BooleanOrDefault.TRUE`, `.FALSE`, `.USE_DEFAULT`.

```kotlin
var enabled: BooleanOrDefault = BooleanOrDefault.USE_DEFAULT

val on = config.enabled or globalDefault
```

### `StringOrDefault`

A string or `default`. `StringOrDefault.USE_DEFAULT`, `StringOrDefault.of("x")`, resolved with
`or("fallback")`.

### `IntOr` / `DoubleOr`

Two flavours each, differing only in the keyword they accept:

| Type | Keyword | Empty constant |
| --- | --- | --- |
| `IntOr.Default` | `default` | `IntOr.Default.USE_DEFAULT` |
| `IntOr.Disabled` | `disabled` | `IntOr.Disabled.DISABLED` |
| `DoubleOr.Default` | `default` | `DoubleOr.Default.USE_DEFAULT` |
| `DoubleOr.Disabled` | `disabled` | `DoubleOr.Disabled.DISABLED` |

Common members: `value: Int?` / `Double?`, `or(fallback)`, and `intValue()` / `doubleValue()`, which
throw `NullPointerException` when the value is empty. `isDefined()` exists on `IntOr` only, not on
`DoubleOr`. The `Disabled` flavours add `enabled()` and `test { … }`, which runs the predicate only
when a value is present.

All four are plain data classes with a public constructor, so `IntOr.Default(5)` or
`DoubleOr.Disabled(2.5)` work in code. `BooleanOrDefault` and `StringOrDefault` instead have private
constructors - use their constants and `StringOrDefault.of`.

```kotlin
@ConfigSerializable
data class RetryConfig(
    var maxRetries: IntOr.Default = IntOr.Default.USE_DEFAULT,
    @field:BelowZeroToEmpty
    var queueLimit: IntOr.Disabled = IntOr.Disabled.DISABLED,
)

val retries = config.maxRetries or 3
if (config.queueLimit.enabled()) {
    val limit = config.queueLimit.intValue()
}
```

`@field:BelowZeroToEmpty` (in `config/type/number/`) makes a negative number read as the empty
state, so an old `view-distance: -1` behaves like `default`.
