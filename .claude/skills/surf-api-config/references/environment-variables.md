# Environment variables

Source: `common/config/api/src/main/kotlin/dev/slne/surf/api/config/environment/` -
`EnvironmentVariables.kt`, `EnvironmentVariableDelegate.kt`, `EnvironmentVariableValidation.kt`,
`EnvironmentVariableException.kt`, `EnvironmentSource.kt`.

This is independent of the config system: no schema, no file, no serializer registry.

## Entry points

```kotlin
import dev.slne.surf.api.config.environment.env
```

`env` is a top-level `@JvmField val` holding `EnvironmentVariables.system`, which reads
`System.getenv`. Other instances:

| Factory | Backing source |
| --- | --- |
| `EnvironmentVariables.system` | `System::getenv` |
| `EnvironmentVariables.from(source: EnvironmentSource)` | your own lookup |
| `EnvironmentVariables.from(values: Map<String, String>)` | an immutable snapshot of the map |

`EnvironmentSource` is a `fun interface` with `operator fun get(name: String): String?`. It must
return `null` **only** for a missing variable; a present-but-blank value has to come back as the
blank string, because that is how the API distinguishes the two. Use `from(map)` in tests instead of
mutating the process environment.

## Direct reads

Every function takes `name` first, and optionally `sensitive: Boolean = false` and a trailing
`validation` block.

| Shape | Required | Optional (`null` when missing) | Defaulted |
| --- | --- | --- | --- |
| String | `required(name)` | `optional(name)` | `string(name, default)` |
| Int | `int(name)` | `optionalInt(name)` | `int(name, default)` |
| Long | `long(name)` | `optionalLong(name)` | `long(name, default)` |
| Double | `double(name)` | `optionalDouble(name)` | `double(name, default)` |
| Float | `float(name)` | `optionalFloat(name)` | `float(name, default)` |
| Boolean | `boolean(name)` | `optionalBoolean(name)` | `boolean(name, default)` |
| UUID | `uuid(name)` | `optionalUuid(name)` | `uuid(name, default)` |
| Duration | `duration(name)` | `optionalDuration(name)` | `duration(name, default)` |
| enum | `enum<T>(name)` | `optionalEnum<T>(name)` | `enum(name, default)` |
| anything | `required(name, converter)` | `optional(name, converter)` | - |

Two extras on top of `required`:

- `requiredNonBlank(name)` - a required string that also rejects a blank value.
- `requiredSecret(name)` - `required(name, sensitive = true)`. It still allows a blank value;
  combine `requiredNonBlank(name, sensitive = true)` when you want both.

```kotlin
val host: String = env.required("DATABASE_HOST")
val port: Int = env.int("DATABASE_PORT", default = 5432) { requireIn(1..65535) }
val instance: UUID? = env.optionalUuid("INSTANCE_ID")
val id: UUID = env.required("ID", UUID::fromString)
val mode: Mode = env.enum("MODE", default = Mode.LOBBY)
```

Conversions worth knowing:

- `boolean` is strict: only `true` / `false`, case-insensitive. `1`, `yes`, `on` all fail.
- `duration` goes through `kotlin.time.Duration.parse`, so ISO-8601 (`PT30S`) and the Kotlin format
  (`30s`, `1h 30m`) work. This is **not** the `ConfigDuration` grammar.
- `enum` matches the constant name exactly, via `enumValueOf<T>`. No case folding, no dashes.
- A custom converter is just `(String) -> T`; anything it throws becomes
  `InvalidEnvironmentVariableException`.

## Property delegates

Call the same function **without** a name. The delegate is lazy
(`lazy(LazyThreadSafetyMode.SYNCHRONIZED)`), caches the parsed result including `null`, and takes
the variable name from the property name **exactly as written** - so the property has to be spelled
like the variable.

```kotlin
object ServiceEnvironment {
    val DATABASE_HOST by env.requiredNonBlank()
    val DATABASE_PORT by env.int(default = 5432) { requireIn(1..65535) }
    val DATABASE_PASSWORD by env.requiredSecret()
    val DEBUG by env.boolean(default = false)
    val INSTANCE_ID by env.optionalUuid()
    val MODE by env.enum<Mode>()
    val apiBase by env.required().named("API_BASE_URL")
}
```

`named(name)` returns an equivalent delegate with an explicit variable name; it validates the name
immediately.

One asymmetry to watch: the delegate overloads of `int`, `long`, `double`, `float`, `boolean`,
`uuid`, `duration` and `enum` take **only** `validation` - no `sensitive` parameter. Only
`required`, `optional`, `requiredNonBlank`, `requiredSecret` and `string` have a `sensitive`
delegate form. `env.int(sensitive = true)` does not compile.

Because resolution is lazy, a missing required variable throws at the **first property read**, not
at class initialization. Touch the object during startup if you want to fail early.

## Validation

The trailing block runs with an `EnvironmentVariableValidation<T>` receiver, after conversion:

- `require(predicate)` - fails with a generic message
- `require(message, predicate)` - fails with your message
- `requireIn(range)` - extension for `Comparable`, inclusive `ClosedRange`

```kotlin
val port by env.int(default = 5432) {
    requireIn(1..65535)
    require("must not be the admin port") { it != 8080 }
}
```

A predicate that throws is wrapped into the validation failure rather than escaping raw.

Validation also runs for **defaulted** reads when the variable is missing - the default value itself
is validated. A default outside your own `requireIn` range throws even though nothing was
configured.

## Missing, blank, unparsable

| Situation | `required…` | `optional…` | defaulted |
| --- | --- | --- | --- |
| variable missing | `MissingEnvironmentVariableException` | `null` | the default, then validated |
| present but blank | returned as-is (`requiredNonBlank` rejects) | returned as-is | converted, so most types throw |
| present, converter fails | `InvalidEnvironmentVariableException` | **throws**, does not return `null` | `InvalidEnvironmentVariableException` |
| validation fails | `EnvironmentVariableValidationException` | same | same |

All three derive from `sealed class EnvironmentVariableException : IllegalStateException`, which
carries `variableName`. `InvalidEnvironmentVariableException` adds `expectedType` and `rawValue`;
`EnvironmentVariableValidationException` adds `reason` and `rawValue`.

`sensitive = true` (and `requiredSecret`) keeps the raw value **and the causing exception** out of
the thrown exception, because parser messages often echo their input. The message then says the
value was not included. Non-sensitive values are escaped and truncated at 128 characters in the
message.

The name itself is validated on every read: empty, or containing `=` or a NUL character, throws
`IllegalArgumentException`.

## What this is not

`env` reads the process environment only. The `.env` file loader in
`common/core/impl/src/main/kotlin/dev/slne/surf/api/core/server/environment/EnvironmentAccessor.kt`
is a separate, impl-side object used by `EnvironmentConfig` to resolve `SURF_ENVIRONMENT` for
`@ConditionalOnEnvironment` (see the `surf-api-components` skill). It also treats a blank value as
absent, which `env` does not. A variable set only in `.env` is invisible to `env`.
