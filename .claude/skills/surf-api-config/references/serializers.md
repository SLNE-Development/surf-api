# The config serializer registry

Source: `common/config/api/src/main/kotlin/dev/slne/surf/api/config/serializer/`,
`platform/*/impl/` (one subclass per platform, see the table).

## How the registry is assembled

`SpongeConfigSerializers` is a bean of surf-api's root context - exactly one `@Service` subclass
per platform:

| Platform | Class |
| --- | --- |
| Paper | `PaperSpongeConfigSerializers` |
| Minestom | `MinestomSpongeConfigSerializer` |
| Velocity | `VelocitySpongeConfigSerializers` |
| Standalone | `StandaloneSpongeConfigSerializers` |

`SpongeConfigManager.open` calls `buildSerializersModule()` once, when a file is opened, and hands
the result to `defaultOptions { options.serializers(…) }`. The order it registers in
matters: **Configurate picks the first registered serializer that matches a type.**

1. everything added through `registerClassSerializer`
2. everything added through `registerTypeTokenSerializer` (seeded with `LinkedList<Any>`)
3. surf's own built-ins (table below)
4. the generic `Map<*, *>` serializer (`MapSerializer(clearInvalids = false)`)
5. `registerAnnotatedObjects(...)` - the `ObjectMapper` factory carrying the Kotlin data-class
   field discoverer and every constraint from `config/constraints/`
6. `registerDefaults(builder)` - the platform subclass's override

So your own registrations beat surf's, and surf's beat the platform's.

## Built-in serializers

| Type | Serializer | Format |
| --- | --- | --- |
| `net.kyori.adventure.text.Component` | `SpongeConfigSerializers.ComponentSerializer` | MiniMessage string, through `SurfMiniMessageHolder.miniMessage()` |
| any `Enum<*>` | `EnumValueSerializer` | constant name; falls back to the name with `_` replaced by `-`. Error message lists up to 10 valid constants |
| `net.kyori.adventure.key.Key` | `KeySerializer` | canonical `namespace:value` |
| `net.kyori.adventure.text.format.TextColor` | `TextColorSerializer` | hex or named colour |
| `ConfigDuration` | `ConfigDuration.Serializer` | `30s`, `5m`, `2h`, `1d` |
| `DurationOrDisabled` | `DurationOrDisabled.Serializer` | a duration or `disabled` |
| `BooleanOrDefault` | `BooleanOrDefault.Serializer` | `true`, `false`, `default` |
| `StringOrDefault` | `StringOrDefault.Serializer` | a string or `default` |
| `IntOr.Default`, `IntOr.Disabled` | nested `Serializer` objects | a number or `default` / `disabled` |
| `DoubleOr.Default`, `DoubleOr.Disabled` | nested `Serializer` objects | a number or `default` / `disabled` |
| `java.util.UUID` | `UuidSerializer` | `UUID.toString()` |
| `kotlin.text.Regex` | `RegexSerializer` | pattern string |
| `java.util.regex.Pattern` | `PatternSerializer` | pattern string |
| `java.net.URI` | `UriSerializer` | URI string |
| `java.net.URL` | `UrlSerializer` | URL string |
| `java.nio.file.Path` | `PathSerializer` | path string |
| `java.io.File` | `FileSerializer` | path string |
| `java.util.LinkedList<T>` | `SpongeConfigSerializers.LinkedListSerializer` | list node |
| `Map<K, V>` | `MapSerializer` | map node, invalid entries skipped |

Everything Configurate itself handles - primitives, strings, `List`, `Set`, arrays, nested
`@ConfigSerializable` classes - still works and needs no entry here.

Each of these throws `SerializationException` on a value it cannot read, which the manager wraps in
`SerializationConfigException` at load time.

### Paper additions

`PaperSpongeConfigSerializers.registerDefaults` adds `NamespacedKey`, `Material`, and - unless the
server is still bootstrapping (`PaperInstance.isBootstrapping()`) - `ItemStack` plus a
`RegistryValueSerializer` for every Bukkit/Paper registry it can resolve: block, item, biome,
structure, enchantment, attribute, sound, damage type, mob effect, menu, dialog, villager
profession and type, trim material and pattern, every mob variant registry, game rule and more. The
namespace `minecraft:` is omitted when writing. Last of all it registers a catch-all for anything
implementing Bukkit's `ConfigurationSerializable`, deliberately late so the specific serializers
above win.

A registry serializer that cannot be built (a registry missing on the running server version) is
swallowed by `registerSafeRegistry`'s `catch (_: Throwable)`, so the type simply has no serializer
instead of breaking startup.

## Adding a serializer for a new type

### From a plugin, for one type

```kotlin
internal object MyTypeSerializer : ScalarSerializer.Annotated<MyType>(MyType::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): MyType =
        MyType.parse(obj.toString())
            ?: throw SerializationException(MyType::class.java, "$obj is not a MyType")

    override fun serialize(
        type: AnnotatedType,
        item: MyType,
        typeSupported: Predicate<Class<*>>
    ): Any = item.toString()
}

@Component
class MySerializers : ConfigSerializerRegistrar {
    override fun register(serializers: SpongeConfigSerializers) {
        serializers.registerClassSerializer(MyTypeSerializer)
    }
}
```

surf-api calls every `ConfigSerializerRegistrar` bean of the plugin's context once while the
context starts, at the latest before the plugin's first `ConfigHandle` opens its file. The
registrars also run when the plugin declares no `@SurfConfig` class. A file opened with
`ConfigHandles.open` before the plugin's context has started does not see the plugin's serializers
yet. The registry is shared
by every plugin on the server: a registration also applies to the configs of other plugins loaded
afterwards.

`registerClassSerializer` has a reified form (`registerClassSerializer<MyType>(serializer)`) and an
explicit one (`registerClassSerializer(MyType::class.java, serializer)`). Remove one again with
`unregisterSerializer(MyType::class.java)`.

Use `ScalarSerializer.Annotated<T>` for a single scalar node, `TypeSerializer.Annotated<T>` when the
value is a map or list node.

### For a generic type

A `Class` is not enough for `Foo<Bar>`; use a `TypeToken`:

```kotlin
override fun register(serializers: SpongeConfigSerializers) {
    serializers.registerTypeTokenSerializer(object : TypeToken<MyBox<String>>() {}, MyBoxSerializer)
}
```

Counterpart: `unregisterTypeTokenSerializer(typeToken)`. Both registries are readable as read-only
`Map`s through `classSerializers` and `typeTokenSerializers`.

### Inside surf-api, platform-wide

Subclass `SpongeConfigSerializers`, override `registerDefaults` (it is
`@MustBeInvokedByOverriders` - call `super.registerDefaults(builder)` first) and annotate the
subclass `@Service`. This is for the platform module, not for a feature: the root context needs
exactly one `SpongeConfigSerializers`, and a second one makes every lookup by type fail with
Spring's `NoUniqueBeanDefinitionException`.

## Map type annotations

Both live in `config/serializer/collection/map/` and go on the **type**, not the field:

```kotlin
@ConfigSerializable
data class Config(
    val strict: @ThrowExceptions Map<String, Int> = emptyMap(),
    val keyed: Map<@WriteKeyBack Key, Int> = emptyMap(),
)
```

- `@ThrowExceptions` - fall back to Configurate's own map serializer, so a bad entry fails the load
  instead of being logged and skipped.
- `@WriteKeyBack` - after deserializing, write the key back in the serializer's canonical form;
  `minecraft:STONE` in the file becomes `minecraft:stone`. The old key is removed.

## Not this registry

`common/serialization/api/src/main/kotlin/dev/slne/surf/api/serialization/` is a **different** thing: those
are `kotlinx.serialization` `KSerializer`s collected in `SurfSerializerModule` (`adventure`,
`spongePoweredMath`, … modules), used for network payloads and persistence, not for config files.
Typealiases such as `SerializableUUID` belong to that set, and the Paper serializers sit in
`SurfPaperSerializerModule`. A serializer there writes a nested UUID, `Component` or `Key` through a
`ContextualSerializer`, so a contextual entry in the format's module overrides its fallback.
Registering one there does not make a type config-serializable, and vice versa.
