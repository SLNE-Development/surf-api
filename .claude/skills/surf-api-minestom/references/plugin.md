# Writing a surf Minestom plugin

Sources: `platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/plugin/`,
`platform/minestom/plugins-impl/`, `local/minestom-example-plugin/` and
`tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/minestom/MinestomSurfPlugin.kt`.

## The Gradle plugin

```kotlin
plugins {
    id("dev.slne.surf.api.gradle.minestom") version "4.0.0"
}

group = "dev.slne.example"
version = "1.0.0"
description = "Shows what a surf Minestom plugin can do"

surfMinestomApi {
    authors.set(listOf("SLNE Development"))
}

minestomPluginFile {
    // id, name, version, description and main already have conventions, see below
}
```

`surfMinestomApi { }` is a `MinestomSurfExtension`, which adds nothing of its own to
`CoreSurfExtension` - the shared settings (`withCoreMinestom()`, `withSurfRedis()`,
`withApiValidation(...)`, `allowInternalApi(...)`, `testing { }`, `addSurfApiToClasspath(...)`, `surfApiScope(...)`,
`basePackage`, `generatePluginClass`, `verifyPluginClasses`, `allowBundledSpring`) are documented in
the `surf-api-paper` skill's `references/project-setup.md`, which describes the same
`CommonSurfPlugin` base and the same plugin-class tasks.

Minestom specifics on top of the common setup:

- the repository `https://central.sonatype.com/repository/maven-snapshots/` is added, restricted to
  snapshots of group `net.minestom`
- `dev.slne.surf.api:surf-api-minestom:+` is added at `compileOnly`
- with testing on (the default), the libraries the surf-api Minestom runtime ships
  (`Constants.COMMON_RUNTIME_TEST_LIBRARIES` and `MINESTOM_RUNTIME_TEST_LIBRARIES`) go on
  `testRuntimeOnly`, and `net.minestom:testing:<version>` goes on `testImplementation`
- the relocations every surf Minestom jar applies are `net.kyori.adventure.nbt` (except
  `...nbt.api.**`), `me.devnatan.inventoryframework` and `org.spongepowered.configurate`, all below
  `dev.slne.surf.api.libs`. These are deliberately identical to the server jar's, so a plugin's
  bytecode names the packages the server really contains.
- `generateSurfPluginClasses` writes `GeneratedSurfPlugin` (extends `SurfMinestomPlugin`) into the
  base package, `build/generated/surf-api/minestom-plugin`, unless `minestomPluginFile { main }`
  names a class. `checkSurfPluginClasses` checks that a named class extends `SurfMinestomPlugin`,
  and `checkNoBundledSpring` fails a shadow jar that bundles Spring, which the server already
  carries.
- the base package, or the package of a named main class, must lie outside `dev.slne.surf.api`
  (`computeSurfBasePackage` fails otherwise): a plugin shaded into the server shares surf-api's
  class loader, and surf-api's root context scans that package.

`dev.slne.surf.api.gradle.minestom-relocations` (`MinestomRelocationsPlugin`) applies only that
relocation list to an existing shadow jar, without configuring a Kotlin/surf project - for an
assembler that merges several surf Minestom plugins into one jar.

## The generated `minestom-plugin.json`

`minestomPluginFile { }` is a `MinestomPluginFile`. The plugin registers the extension and gives it
conventions, so in practice nothing has to be set:

| Property | Convention |
| --- | --- |
| `id` | `project.name.lowercase()`; must match `[a-z][a-z0-9-_]{0,63}` |
| `main` | `<basePackage>.GeneratedSurfPlugin`; a class named here wins |
| `name` | `project.name` |
| `version` | `project.version` |
| `description` | `project.description` or empty |
| `url` | the `url` Gradle property, else the `origin` Git remote as an `https` URL |
| `authors` | `surfMinestomApi { authors }` |
| `pluginDependencies` | `surf-core-minestom`, enabled only when a core module was set |

`validate()` rejects a missing or malformed `id`, a blank `version`, a missing `main` and a blank
dependency name. Without a main class - none named and `generatePluginClass = false`, for a module
that is not a plugin itself - the generators write nothing instead of failing.

Three resources are generated into `main`'s resources, by tasks in the `surf-api` group:

| Task | File | Why |
| --- | --- | --- |
| `generateMinestomPluginFile` | `minestom-plugin.json` | what the server reads for a jar in `plugins/` |
| `generateMinestomPluginIndex` | `META-INF/services/dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin` | survives several plugins being shaded into one jar |
| `generateMinestomPluginClassPathFile` | `META-INF/surf-minestom-plugins/<main>.json` | the plugin file again, named after the main class |

The last two are how a plugin shaded onto the server's own class path is still found: a single
`minestom-plugin.json` would be overwritten when two plugins are merged, so the service entry names
the main class and the per-class descriptor carries that plugin's metadata.

`local/minestom-example-plugin` writes all three by hand and names its own `ExamplePlugin`, because
the published Gradle plugin is not available inside this build - its `build.gradle.kts` says so
explicitly. Use it as a sample of the file contents, not as a template for a downstream build
script.

A dependency is written as `{"id": "<id>", "optional": <bool>}`. Declare an optional one with:

```kotlin
minestomPluginFile {
    pluginDependencies {
        register("economy") { optional = true }
    }
}
```

The `<id>?` suffix belongs only to a server jar's `Surf-Plugin-Dependencies` manifest attribute.

## The plugin class

`SurfMinestomPlugin` (`api/.../plugin/SurfMinestomPlugin.kt`) is abstract, with five properties and
no lifecycle methods:

| Member | What it is |
| --- | --- |
| `meta: PluginMeta` | what the plugin's `minestom-plugin.json` says |
| `eventNode: EventNode<Event>` | the plugin's own node; removed with all listeners on disable |
| `dataDirectory: Path` | `plugins/<id>`, created before the plugin is constructed |
| `logger: ComponentLogger` | the plugin's logger, named after the plugin |
| `scope: CoroutineScope` | the plugin's coroutine scope, cancelled when the plugin is disabled |

The class needs a constructor without parameters, or has to be an `object`. The context is taken in
the constructor, so property initialisers - including those of an `object`, which run in its static
initialiser - can already use `meta`, `logger` and the rest. Touching them on an instance the server
did not create throws `"<class> has not been initialized by the server yet"`.

The instance is the primary bean of the plugin's Spring context, registered under its concrete
type. Spring manages it like any other singleton: `@Autowired` members, `@PostConstruct`, every
`BeanPostProcessor`, and `@PreDestroy` and `DisposableBean.destroy` when the context closes.

The scope is the plugin context's primary `CoroutineScope` bean too, so a bean can inject it instead
of going through the plugin. It has a `SupervisorJob`, runs on `Dispatchers.Default`, is named
`surf-<id>`, and makes the plugin's class loader (the server's, for a plugin without a jar of its
own) the thread context class loader of its coroutines. An uncaught failure in it is logged by the
commons-logging logger `surf-coroutines` as `Unhandled exception in a coroutine of '<id>'`, not by
the plugin's `logger`.

### The beans

The component scan starts at the main class's package and reads only the plugin's jar, or, for a
plugin shaded into the server, every class under that package. From
`local/minestom-example-plugin/src/main/kotlin/dev/slne/minestom/example/plugin/`:

| File | Bean | What it does |
| --- | --- | --- |
| `ExamplePlugin.kt` | the plugin instance | `class ExamplePlugin : SurfMinestomPlugin()`, nothing else |
| `Greeting.kt` | `@Service`, `SurfLifecycle` | reads `greeting.txt` from `plugin.dataDirectory` in `onLoad` |
| `Heartbeat.kt` | `@Service`, `SurfLifecycle` | launches a loop in `plugin.scope` in `onEnable`, logs in `onDisable` |
| `ExampleEvents.kt` | `@Configuration` with an `@Bean` `EventNode<Event>` | greets joining players; surf-api attaches the node on enable |
| `ExampleCommands.kt` | `@SpringComponent`, `CommandRegistrar` | registers `/plugins` in `register()` |

Spring's `@Component` clashes with Adventure's `Component` in one file, so the example annotates the
bean with surf-api's typealias `SpringComponent`:

```kotlin
import dev.slne.surf.api.spring.SpringComponent

@SpringComponent
class ExampleCommands : CommandRegistrar {

    override fun register() {
        commandAPICommand("plugins") {
            playerExecutor { player, _ ->
                val plugins = PluginManager.instance.plugins.joinToString { "${it.meta.name} ${it.meta.version}" }
                player.sendMessage(Component.text("Plugins: $plugins"))
            }
        }
    }
}
```

The phases, per plugin:

| Phase | What surf-api does |
| --- | --- |
| load | creates the plugin, builds its Spring context below surf-api's root, runs the `SurfLifecycle.onLoad` hooks |
| enable | attaches the `EventNode` beans to `eventNode`, registers the command beans, `CommandRegistrar` beans and `@SurfEventHandler` beans, runs `onEnable` |
| disable | runs `onDisable` in reverse order, removes what enable registered, closes the context |

A plugin injects the `@PluginApi` beans of the plugins listed as its `dependencies` in
`minestom-plugin.json` (`minestomPluginFile { pluginDependencies { } }`); everything else of another
plugin stays private. See **`surf-api-components`**.

## How the server loads it

From the doc comment on `PluginsFeature` (`platform/minestom/plugins-impl/.../PluginsFeature.kt`):

- The feature loads plugins from the `plugins` directory **and** those shaded into the server.
- Each jar from `plugins/` gets its own class loader, which sees the server, the plugin itself and
  the plugins it depends on - but no other plugin.
- Each plugin gets its own Spring context, which sees surf-api's beans and the `@PluginApi` beans of
  the plugins it depends on.
- Plugins load in dependency order while the server starts, are enabled right after loading (before
  `surfMinestomServer` returns, so before players can join) and are disabled in reverse on shutdown.
- `<directory>/<id>` is created before the plugin is constructed.
- A plugin that fails to load or enable is skipped and logged, unless
  `withPlugins { requireAll = true }`. Failing to load includes a context that cannot be built and a
  throwing `onLoad` hook; failing to enable includes a throwing `onEnable` hook. Plugins that
  require a failed plugin are skipped too.
- A jar in the directory without a `minestom-plugin.json` is not a plugin at all.
- `PluginsFeature.loadPriority` is `Int.MAX_VALUE`, so plugins load after every other feature and
  can use them.

The server's own code **cannot** see into a plugin class loader. To use a plugin's classes from the
server, list it in `surfMinestomServerApi { pluginDependencies { } }` - the bootstrap then puts that
plugin and its dependencies on the system class path - and add it as a `compileOnly` dependency.

`PluginManager.instance` throws `IllegalStateException("This server does not load plugins")` when
the plugins feature is not installed. `plugins`, `plugin(id)` and `isLoaded(id)` are the lookups.
