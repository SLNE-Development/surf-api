# Config serializers

Source: `common/config/api/src/main/kotlin/dev/slne/surf/api/config/serializer/`,
`common/config/impl/src/main/kotlin/dev/slne/surf/api/config/impl/serializer/` and
`platform/paper/impl/.../server/config/serializers/`.

## How a file's serializers are assembled

Every serializer is a bean whose class carries `@SurfConfigSerializer`, which is meta-annotated
with `@Component`. There is no registry to write into. Each config file gets its own
`TypeSerializerCollection`, built when its handle is created (`RootConfigSerializers.collection`
in config-impl). **Configurate picks the first registered serializer that matches a type**, and
the collection registers, in this order:

1. the file's own serializers: `@SurfConfig(serializers = [...])`, or the `serializers` list
   passed to `ConfigHandles.open`
2. the plugin's `applyToAll` serializers - only beans the plugin context declares itself, never
   those of its import context or of other plugins (`PluginConfigSerializers`)
3. the root defaults - the `applyToAll` serializer beans of surf-api's root context and the
   serializers of every `DefaultConfigSerializerSource` bean there (Paper's registry serializers)
4. `registerAnnotatedObjects(ConfigObjectMappers.createFactory())` - the object mapper with the
   Kotlin data-class field discoverer and every constraint from `config/constraints/`
5. Configurate's own defaults, as the parent collection

`ConfigHandles.open` skips level 2: plugin serializers never apply to files opened that way.

Within one level a serializer for a subtype is registered before one for its supertype, so the most
specific serializer wins regardless of scan order: `NamespacedKeySerializer` beats `KeySerializer`,
`ItemStackSerializer` beats the `ConfigurationSerializable` catch-all. Across levels the higher
level wins, so a plugin serializer for a supertype also shadows root serializers for its subtypes.

Two serializers of one level for the same type fail: in a plugin, the plugin's start (checked once
per context, also without `@SurfConfig` classes); in the root, the root's start; in the list
passed to `ConfigHandles.open`, that call with `IllegalArgumentException`. The message names both
classes.

### Which type a serializer serializes

`ConfigSerializerRegistration.of` resolves it in this order:

1. `@SurfConfigSerializer(type = X::class)`, when not the default `Nothing::class`
2. `type()` of a `ScalarSerializer`
3. the type argument of the class's `TypeSerializer<T>` supertype, resolved with geantyref, so
   `TypeSerializer<List<Price>>` serializes `List<Price>`

A type argument that is or contains a type variable fails with "Cannot resolve the type that config
serializer … serializes". Like every Configurate registration, the serializer also matches
subtypes of its type.

## Built-in serializers

All of them are `internal` classes with `@SurfConfigSerializer`, scanned into the root context.

| Type | Serializer | Format |
| --- | --- | --- |
| `net.kyori.adventure.text.Component` | `ComponentSerializer` | MiniMessage string, through `SurfMiniMessageHolder.miniMessage()` |
| any `Enum<*>` | `EnumValueSerializer` | constant name; falls back to the name with `_` replaced by `-`. Error message lists up to 10 valid constants |
| `net.kyori.adventure.key.Key` | `KeySerializer` | canonical `namespace:value` |
| `net.kyori.adventure.text.format.TextColor` | `TextColorSerializer` | hex or named colour |
| `ConfigDuration` | `ConfigDuration.Serializer` | `30s`, `5m`, `2h`, `1d` |
| `DurationOrDisabled` | `DurationOrDisabled.Serializer` | a duration or `disabled` |
| `BooleanOrDefault` | `BooleanOrDefault.Serializer` | `true`, `false`, `default` |
| `StringOrDefault` | `StringOrDefault.Serializer` | a string or `default` |
| `IntOr.Default`, `IntOr.Disabled` | nested `Serializer` classes | a number or `default` / `disabled` |
| `DoubleOr.Default`, `DoubleOr.Disabled` | nested `Serializer` classes | a number or `default` / `disabled` |
| `java.util.UUID` | `UuidSerializer` | `UUID.toString()` |
| `kotlin.text.Regex` | `RegexSerializer` | pattern string |
| `java.util.regex.Pattern` | `PatternSerializer` | pattern string |
| `java.net.URI` | `UriSerializer` | URI string |
| `java.net.URL` | `UrlSerializer` | URL string |
| `java.nio.file.Path` | `PathSerializer` | path string |
| `java.io.File` | `FileSerializer` | path string |
| `java.util.LinkedList<Any>` | `LinkedListSerializer` | list node |
| `Map<*, *>` | `MapSerializer` | map node, invalid entries skipped |

Everything Configurate itself handles - primitives, strings, `List`, `Set`, arrays, nested
`@ConfigSerializable` classes - still works and needs no entry here.

Each of these throws `SerializationException` on a value it cannot read, which the manager wraps in
`SerializationConfigException` at load time.

### Paper additions

Paper's root adds `NamespacedKeySerializer`, `MaterialSerializer`, `ItemStackSerializer` and
`BukkitConfigurationSerializableSerializer`, a catch-all for anything implementing Bukkit's
`ConfigurationSerializable`, such as `Location`. The specificity order keeps the catch-all behind
`ItemStack`.

`RegistryConfigSerializers` (`DefaultConfigSerializerSource`) supplies a `RegistryValueSerializer`
for every Bukkit/Paper registry it can resolve: block, item, biome, structure, enchantment,
attribute, sound, damage type, mob effect, menu, dialog, villager profession and type, trim
material and pattern, every mob variant registry, game rule and more. The namespace `minecraft:` is
omitted when writing. It supplies nothing while `PaperInstance.isBootstrapping()`, and skips a
registry that cannot be built on the running server version (`catch (_: Throwable)`), so the type
simply has no serializer instead of breaking startup. It is asked again for every file.

## Adding a serializer for a new type

### From a plugin

```kotlin
@SurfConfigSerializer
class MyTypeSerializer : ScalarSerializer.Annotated<MyType>(MyType::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): MyType {
        return MyType.parse(obj.toString())
            ?: throw SerializationException(MyType::class.java, "$obj is not a MyType")
    }

    override fun serialize(
        type: AnnotatedType,
        item: MyType,
        typeSupported: Predicate<Class<*>>,
    ): Any = item.toString()
}
```

- The class is a bean of the plugin, so its constructor may inject other beans. surf-api never
  creates it. Declare a class, not an `object`: Spring creates the instance.
- It applies to every `@SurfConfig` file of the plugin, and overrides a root default for the same
  type within the plugin.
- `@SurfConfigSerializer(applyToAll = false)` limits it to the files that name it:
  `@SurfConfig("shop.yml", serializers = [MyTypeSerializer::class])`. A named class must be a
  `@SurfConfigSerializer` bean of the plugin context or of the root context; anything else fails
  the start with "… is no @SurfConfigSerializer bean of plugin '…' or of surf-api's root context",
  also for a lazy config.
- Use `ScalarSerializer.Annotated<T>` for a single scalar node, `TypeSerializer.Annotated<T>` when
  the value is a map or list node.

### For a generic type

Declare the full type in the supertype: `class MyBoxSerializer : TypeSerializer<MyBox<String>>`.
When the class itself is generic, the type cannot be resolved; give it explicitly with
`@SurfConfigSerializer(type = MyBox::class)`, which registers for the raw class.

### For `ConfigHandles.open`

Pass instances: `ConfigHandles.open<C>(path, serializers = listOf(MyTypeSerializer()))`. They are
resolved like beans, come before the root defaults, and need no annotation.

### Inside surf-api

A root default is an `internal` class with `@SurfConfigSerializer` in a package the root context
scans (`dev.slne.surf.api...`). A family of serializers built from runtime data, such as one per
registry, goes into an `@InternalSurfApi` `DefaultConfigSerializerSource` component instead. A
duplicate type among the annotated root defaults fails the root context's start. A source's output
is validated each time a file's serializers are built, so a duplicate involving it fails only then;
Paper's registry source supplies nothing during the bootstrap phase.

surf-api scans its own package with fully qualified bean names, so an application hosted in the
root context may declare a serializer class with the same simple name, such as `UuidSerializer`.

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

## Not these serializers

`common/serialization/api/src/main/kotlin/dev/slne/surf/api/serialization/` is a **different** thing: those
are `kotlinx.serialization` `KSerializer`s collected in `SurfSerializerModule` (`adventure`,
`spongePoweredMath`, … modules), used for network payloads and persistence, not for config files.
Typealiases such as `SerializableUUID` belong to that set, and the Paper serializers sit in
`SurfPaperSerializerModule`. A serializer there writes a nested UUID, `Component` or `Key` through a
`ContextualSerializer`, so a contextual entry in the format's module overrides its fallback.
Registering one there does not make a type config-serializable, and vice versa.
