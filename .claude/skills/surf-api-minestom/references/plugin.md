# Writing a surf Minestom plugin

Sources: `platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/plugin/`,
`platform/minestom/plugins-impl/`, `local/minestom-example-plugin/` and
`tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/minestom/MinestomSurfPlugin.kt`.

## The Gradle plugin

```kotlin
plugins {
    id("dev.slne.surf.api.gradle.minestom") version "3.0.0"
}

group = "dev.slne.example"
version = "1.0.0"
description = "Shows what a surf Minestom plugin can do"

surfMinestomApi {
    authors.set(listOf("SLNE Development"))
}

minestomPluginFile {
    main = "dev.slne.example.lobby.LobbyPlugin"
    // id, name, version and description already have conventions, see below
}
```

`surfMinestomApi { }` is a `MinestomSurfExtension`, which adds nothing of its own to
`CoreSurfExtension` - the shared settings (`withCoreMinestom()`, `withSurfRedis()`,
`withApiValidation()`, `testing { }`, `addSurfApiToClasspath(...)`, `surfApiScope(...)`) are
documented in the `surf-api-paper` skill's `references/project-setup.md`, which describes the same
`CommonSurfPlugin` base.

Minestom specifics on top of the common setup:

- the repository `https://central.sonatype.com/repository/maven-snapshots/` is added, restricted to
  snapshots of group `net.minestom`
- `dev.slne.surf.api:surf-api-minestom:+` is added at `compileOnly`
- with testing on (the default), `net.minestom:testing:<version>` goes on `testImplementation`
- the relocations every surf Minestom jar applies are `com.mojang.serialization`,
  `com.mojang.datafixers`, `net.kyori.adventure.nbt` (except `...nbt.api.**`),
  `me.devnatan.inventoryframework` and `org.spongepowered.configurate`, all below
  `dev.slne.surf.api.libs`. These are deliberately identical to the server jar's, so a plugin's
  bytecode names the packages the server really contains.

`dev.slne.surf.api.gradle.minestom-relocations` applies only that relocation list to an existing
shadow jar, without configuring a Kotlin/surf project - for an assembler that merges several surf
Minestom plugins into one jar.

## The generated `minestom-plugin.json`

`minestomPluginFile { }` is a `MinestomPluginFile`. The plugin registers the extension and gives it
conventions, so in practice only `main` has to be set:

| Property | Convention |
| --- | --- |
| `id` | `project.name.lowercase()`; must match `[a-z][a-z0-9-_]{0,63}` |
| `main` | none - **required** |
| `name` | `project.name` |
| `version` | `project.version` |
| `description` | `project.description` or empty |
| `url` | the `url` Gradle property |
| `authors` | `surfMinestomApi { authors }` |
| `pluginDependencies` | `surf-core-minestom`, enabled only when a core module was set |

`validate()` rejects a missing or malformed `id`, a blank `version`, a missing `main` and a blank
dependency name. `isApplied()` is `main.isPresent`: without a main class the generators log a
warning and write nothing instead of failing.

Three resources are generated into `main`'s resources, by tasks in the `surf-api` group:

| Task | File | Why |
| --- | --- | --- |
| `generateMinestomPluginFile` | `minestom-plugin.json` | what the server reads for a jar in `plugins/` |
| `generateMinestomPluginIndex` | `META-INF/services/dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin` | survives several plugins being shaded into one jar |
| `generateMinestomPluginClassPathFile` | `META-INF/surf-minestom-plugins/<main>.json` | the plugin file again, named after the main class |

The last two are how a plugin shaded onto the server's own class path is still found: a single
`minestom-plugin.json` would be overwritten when two plugins are merged, so the service entry names
the main class and the per-class descriptor carries that plugin's metadata.

`local/minestom-example-plugin` writes all three by hand, because the published Gradle plugin is not
available inside this build - its `build.gradle.kts` says so explicitly. Use it as a sample of the
file contents, not as a template for a downstream build script.

A dependency is written as `"<id>"`, or `"<id>?"` when optional.

## The plugin class

`SurfMinestomPlugin` (`api/.../plugin/SurfMinestomPlugin.kt`) is abstract with three open suspending
functions and five properties:

| Member | What it is |
| --- | --- |
| `meta: PluginMeta` | what the plugin's `minestom-plugin.json` says |
| `eventNode: EventNode<Event>` | the plugin's own node; removed with all listeners on disable |
| `dataDirectory: Path` | `plugins/<id>`, created before the plugin is constructed |
| `logger: ComponentLogger` | the plugin's logger |
| `scope: CoroutineScope` | cancelled when the plugin is disabled |
| `onLoad()` | while the server starts |
| `onEnable()` | once every plugin is loaded, still while the server starts |
| `onDisable()` | at the latest on shutdown |

The context is taken in the constructor, so property initialisers - including those of an `object`,
which run in its static initialiser - can already use `meta`, `logger` and the rest. Touching them
on an instance the server did not create throws
`"<class> has not been initialized by the server yet"`.

From `local/minestom-example-plugin/.../ExamplePlugin.kt`:

```kotlin
class ExamplePlugin : SurfMinestomPlugin() {
    private lateinit var greeting: String

    override suspend fun onLoad() {
        val file = dataDirectory.createDirectories().resolve("greeting.txt")
        if (!file.exists()) file.writeText("Welcome to the surf api example server!")
        greeting = file.readText().trim()
    }

    override suspend fun onEnable() {
        eventNode.addListener<PlayerJoinEvent> { event ->
            event.player.sendMessage(Component.text(greeting, NamedTextColor.GREEN))
        }

        commandAPICommand("plugins") {
            playerExecutor { player, _ ->
                val plugins = PluginManager.instance.plugins
                    .joinToString { "${it.meta.name} ${it.meta.version}" }
                player.sendMessage(Component.text("Plugins: $plugins"))
            }
        }

        scope.launch {
            while (true) {
                delay(300.seconds)
                logger.info("{} is still running", meta.name)
            }
        }
    }

    override suspend fun onDisable() {
        logger.info("Goodbye from {}", meta.name)
    }
}
```

## How the server loads it

From the doc comment on `PluginsFeature` (`platform/minestom/plugins-impl/.../PluginsFeature.kt`):

- The feature loads plugins from the `plugins` directory **and** those shaded into the server.
- Each jar from `plugins/` gets its own class loader, which sees the server, the plugin itself and
  the plugins it depends on - but no other plugin.
- Plugins load in dependency order while the server starts, are enabled right after loading (before
  `surfMinestomServer` returns, so before players can join) and are disabled in reverse on shutdown.
- `<directory>/<id>` is created before the plugin is constructed.
- A plugin that fails to load or enable is skipped and logged, unless
  `withPlugins { requireAll = true }`.
- A jar in the directory without a `minestom-plugin.json` is not a plugin at all.
- `PluginsFeature.loadPriority` is `Int.MAX_VALUE`, so plugins load after every other feature and
  can use them.

The server's own code **cannot** see into a plugin class loader. To use a plugin's classes from the
server, list it in `surfMinestomServerApi { pluginDependencies { } }` - the bootstrap then puts that
plugin and its dependencies on the system class path - and add it as a `compileOnly` dependency.

`PluginManager.instance` throws `IllegalStateException("This server does not load plugins")` when
the plugins feature is not installed. `plugins`, `plugin(id)` and `isLoaded(id)` are the lookups.
