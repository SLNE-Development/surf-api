package dev.slne.surf.api.di.boot

import dev.slne.surf.api.di.BeanContainer
import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.PlatformLogger
import dev.slne.surf.api.di.Scope
import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.definition.BeanMetadata
import dev.slne.surf.api.di.definition.FactoryDefinition
import dev.slne.surf.api.di.definition.FactoryKind
import dev.slne.surf.api.di.definition.InjectionPoint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.Enumeration
import javax.tools.ToolProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Exercises the container hierarchy under class loader topologies that reproduce the delegation
 * order of the platforms surf-api runs on.
 *
 * The whole cross-application story rests on one property of the JVM rather than on anything the
 * container does: a [Scope.SHARED] bean crosses an application boundary as an *interface*, and that
 * interface has to be loaded by exactly one class loader. Two applications that each load their own
 * copy hold two unrelated [Class] objects with the same binary name — `isAssignableFrom` is false in
 * both directions — and the container, which matches providers to injection points by *name*, hands
 * the consumer an instance it cannot cast. Nothing in the container can detect that: the metadata
 * records names, the resolver compares names, and the injected value is only ever an `Any` until the
 * consumer's own bytecode casts it.
 *
 * So the cases here are about class identity, not about the container's bookkeeping. Each one builds
 * the loaders by hand and asserts what *the plugin's own loader* thinks the interface is, which is
 * the only assertion that can tell the good topology from the broken one — object identity holds
 * either way.
 *
 * ### The two delegation orders, and why the topologies differ
 *
 * Paper's `PaperPluginClassLoader.loadClass(name, resolve, checkGlobal, checkLibraries)` tries, in
 * order: `super.loadClass` (a plain parent-first [URLClassLoader]: the server, then this plugin's
 * own jar), this plugin's own library loader, the plugin's class loader group, then
 * [ClassNotFoundException]. A plugin's own jar therefore beats the group — and surf-api, on Paper,
 * *is* a plugin, so it is only ever reachable through the group. That is the whole reason a plugin
 * which shades the shared interface breaks sharing, and the reason surf-api's
 * `has-open-classloader: false` is modelled here as an empty group on surf-api's own loader: a
 * plugin reaches surf-api because it declares it, and nothing is ever added to surf-api's side.
 *
 * Minestom's `PluginClassLoader` is parent-first all the way — `findLoadedClass`, the parent, the
 * own jar, the declared dependencies — and surf-api is part of the server's own class path there, so
 * the server's copy of an interface wins before the plugin's jar is ever consulted. Same shading, no
 * breakage.
 *
 * ### Why the interface is compiled during the test
 *
 * Two genuinely distinct [Class] objects with the same binary name are the point. Two differently
 * named interfaces would prove nothing, and the test source cannot name a class that does not exist
 * when the test source is compiled. The three Java classes in [ISOLATION_SOURCES] are therefore
 * compiled by the JDK compiler into a temporary directory and the resulting class files are copied
 * into the entry each loader owns, which is what makes a loader define its own copy.
 * `isolation.Consumer` matters as much as the interface: it is real bytecode, in the plugin's own
 * entry, performing the real cast, so the failure a mismatched topology produces is the JVM's own
 * [ClassCastException] and not something this test simulates.
 */
class ClassLoaderIsolationTest {

    @BeforeTest
    fun forgetThePublishedInstance() {
        publishedGreeting = null
    }

    // --- the good case --------------------------------------------------------------------------

    @Test
    fun `a shared bean reaches both plugins as the one interface surf-api loaded`(
        @TempDir temp: Path,
    ) = runTest {
        val fixture = IsolationFixture(temp)
        val paper = PaperTopology(fixture)
        val containers = paper.start()

        val shared = assertNotNull(publishedGreeting, "the root's factory published an instance")
        val fromA = containers.pluginA.bean(PluginABean::class).greeting
        val fromB = containers.pluginB.bean(PluginBBean::class).greeting

        assertNotNull(containers.root.beanOrNull(GreetingPublisher::class), "the root holds its own")
        assertSame(shared, fromA, "plugin A is injected the instance the root published")
        assertSame(fromA, fromB, "and so is plugin B: one SHARED instance for the process")

        // The assertions that actually test class identity. Everything above would hold just as
        // well if the two plugins saw two unrelated interfaces of the same name.
        assertSame(paper.surfApi.loadClass(GREETING), paper.pluginA.loadClass(GREETING))
        assertSame(paper.surfApi.loadClass(GREETING), paper.pluginB.loadClass(GREETING))
        assertTrue(
            paper.pluginA.loadClass(GREETING).isInstance(fromA),
            "the injected value has to be assignable to the interface as plugin A's loader sees it",
        )
        assertTrue(
            paper.pluginB.loadClass(GREETING).isInstance(fromB),
            "and to the interface as plugin B's loader sees it",
        )

        // And the cast is performed by each plugin's own compiled code, not by the test.
        assertEquals(GREETING_TEXT, consume(paper.pluginA, fromA))
        assertEquals(GREETING_TEXT, consume(paper.pluginB, fromB))
    }

    // --- the failure the design is arranged to avoid ---------------------------------------------

    @Test
    fun `a plugin that bundles the interface is handed an instance it cannot cast, and nothing says so`(
        @TempDir temp: Path,
    ) = runTest {
        val fixture = IsolationFixture(temp)

        // Plugin B's own entry carries a copy of the interface, which is what shading surf-api into
        // a plugin jar amounts to. Paper consults that entry in step 1 and the group only in step 3.
        val paper = PaperTopology(fixture, bundledByPluginB = listOf(GREETING))
        val containers = paper.start()

        val shared = assertNotNull(publishedGreeting)
        val surfApiCopy = paper.surfApi.loadClass(GREETING)
        val pluginBCopy = paper.pluginB.loadClass(GREETING)

        assertEquals(surfApiCopy.name, pluginBCopy.name, "the same binary name throughout")
        assertNotSame(surfApiCopy, pluginBCopy, "but plugin B's own entry wins over its group")
        assertFalse(
            surfApiCopy.isAssignableFrom(pluginBCopy),
            "two copies of one interface are unrelated types, however identical their source",
        )
        assertFalse(pluginBCopy.isAssignableFrom(surfApiCopy), "and the relation holds neither way")

        val injected = containers.pluginB.bean(PluginBBean::class).greeting

        assertSame(shared, injected, "the container still hands out the one shared instance")
        assertTrue(surfApiCopy.isInstance(injected), "it implements surf-api's copy")
        assertFalse(
            pluginBCopy.isInstance(injected),
            "and not plugin B's, which is the whole defect: the bean is useless to its consumer",
        )

        // What plugin B's own bytecode does with the value it was given.
        val failure = assertFailsWith<ClassCastException> { consume(paper.pluginB, injected) }
        val message = failure.message.orEmpty()

        assertContains(message, GREETING)
        assertContains(
            message,
            "surf-api",
            message = "the JVM names the loader that defined the instance",
        )
        assertContains(
            message,
            "plugin-b",
            message = "and the loader that defined the expected interface",
        )

        // The finding this case exists to record: the container completed both starts, logged
        // nothing about it, and could not have. Resolution matches names, so a plugin holding its
        // own copy of an interface is indistinguishable from one that does not until the consumer
        // casts — by which time the container is long done.
        assertEquals(emptyList(), fixture.logger.warnings, "no warning was raised")
        assertEquals(emptyList(), fixture.logger.errors, "no failure was reported")
        assertFalse(
            fixture.logger.lines.any { GREETING in it },
            "nothing the container logged even mentions the interface: ${fixture.logger.lines}",
        )
    }

    // --- per-application bean discovery ----------------------------------------------------------

    @Test
    fun `a child container holds its own application's beans and resolves the rest upwards`(
        @TempDir temp: Path,
    ) = runTest {
        val fixture = IsolationFixture(temp)
        val paper = PaperTopology(fixture)
        val containers = paper.start()

        assertNotNull(containers.pluginA.beanOrNull(PluginABean::class), "A holds what A declared")
        assertNotNull(containers.pluginB.beanOrNull(PluginBBean::class), "B holds what B declared")
        assertNull(
            containers.pluginA.beanOrNull(PluginBBean::class),
            "A must not see B's bean: each loader serves only its own metadata",
        )
        assertNull(containers.pluginB.beanOrNull(PluginABean::class), "nor B A's")

        // Resolution walks up to the root, never sideways between siblings.
        assertSame(
            containers.root.bean(GreetingPublisher::class),
            containers.pluginA.bean(GreetingPublisher::class),
            "a lookup a child cannot answer is answered by the root",
        )
        assertSame(
            containers.pluginA.bean(GreetingPublisher::class),
            containers.pluginB.bean(GreetingPublisher::class),
            "and both children reach the same one",
        )
    }

    // --- the root cannot see a child ------------------------------------------------------------

    @Test
    fun `the root resolves none of the plugins' beans and cannot even load their classes`(
        @TempDir temp: Path,
    ) = runTest {
        val fixture = IsolationFixture(temp)
        val paper = PaperTopology(fixture)
        val containers = paper.start()

        assertNull(containers.root.beanOrNull(PluginABean::class), "the root holds only surf-api's")
        assertNull(containers.root.beanOrNull(PluginBBean::class))

        // Not a filter the container applies — the loader genuinely cannot reach the classes.
        // surf-api's group is empty, which is what `has-open-classloader: false` buys, so a
        // plugin's class is not even nameable from the root's loader.
        assertFailsWith<ClassNotFoundException> { paper.surfApi.loadClass(CONSUMER) }

        // The asymmetry is the point: the plugins reach surf-api because they declare it.
        assertSame(
            paper.surfApi.loadClass(GREETING),
            paper.pluginA.loadClass(GREETING),
            "a plugin's group holds surf-api's loader; surf-api's group holds nothing",
        )
    }

    // --- Minestom's parent-first loader ---------------------------------------------------------

    @Test
    fun `a parent-first loader keeps sharing intact even when the plugin bundles the interface`(
        @TempDir temp: Path,
    ) = runTest {
        val fixture = IsolationFixture(temp)

        // On Minestom surf-api is part of the server's own class path, so the root is built on the
        // server's loader and the plugin's loader has it as its parent.
        val server = fixture.server(
            "minestom-server",
            classes = listOf(GREETING, GREETING_IMPL),
            beans = listOf(publisherDefinition()),
        )
        val plugin = fixture.minestomPlugin(
            "minestom-plugin",
            parent = server,
            // The same shading that breaks sharing on Paper.
            classes = listOf(CONSUMER, GREETING),
            beans = listOf(pluginBeanDefinition(PluginBBean::class.java)),
        )

        publishedGreeting = newGreeting(server)

        fixture.host.startRoot(fixture.owner("surf-api", server))
        val child = fixture.host.startChild(fixture.owner("minestom-plugin", plugin))

        assertSame(
            server.loadClass(GREETING),
            plugin.loadClass(GREETING),
            "the parent is consulted before the plugin's own jar, so the bundled copy is dead weight",
        )

        val injected = child.bean(PluginBBean::class).greeting

        assertSame(publishedGreeting, injected)
        assertTrue(plugin.loadClass(GREETING).isInstance(injected), "sharing holds")
        assertEquals(GREETING_TEXT, consume(plugin, injected), "and the plugin's own cast succeeds")
        assertEquals(emptyList(), fixture.logger.errors)
    }
}

// --- the topology -------------------------------------------------------------------------------

/**
 * The Paper topology: a server loader, surf-api as a closed plugin beside the others, and two
 * plugins that declare it.
 *
 * Built eagerly, because the loaders have to exist before the containers do — the platform is asked
 * for an owner's loader during [SurfContainerHost.startRoot] and [SurfContainerHost.startChild], and
 * it is that answer the whole test turns on.
 *
 * @param bundledByPluginB class names plugin B's own entry carries on top of its own classes; adding
 * [GREETING] here is a plugin that shades the shared interface
 */
private class PaperTopology(
    private val fixture: IsolationFixture,
    bundledByPluginB: List<String> = emptyList(),
) {

    /** The server's own class path, which carries none of surf-api's classes on this platform. */
    val server = fixture.server("paper-server")

    /** surf-api's plugin loader, which owns the one copy of the shared interface. */
    val surfApi = fixture.paperPlugin(
        "surf-api",
        parent = server,
        classes = listOf(GREETING, GREETING_IMPL),
        beans = listOf(publisherDefinition()),
    )

    /** A plugin carrying nothing but its own class. */
    val pluginA = fixture.paperPlugin(
        "plugin-a",
        parent = server,
        classes = listOf(CONSUMER),
        beans = listOf(pluginBeanDefinition(PluginABean::class.java)),
    )

    /** A plugin carrying its own class plus whatever a case told it to bundle. */
    val pluginB = fixture.paperPlugin(
        "plugin-b",
        parent = server,
        classes = listOf(CONSUMER) + bundledByPluginB,
        beans = listOf(pluginBeanDefinition(PluginBBean::class.java)),
    )

    init {
        // A plugin reaches surf-api because its own manifest declares it. Nothing is added to
        // surf-api's own group: its `has-open-classloader: false` keeps its loader out of the open
        // group, so no downstream plugin ever appears on surf-api's side of the relation.
        pluginA.group = listOf(surfApi)
        pluginB.group = listOf(surfApi)

        publishedGreeting = newGreeting(surfApi)
    }

    /** Starts the root and both children, in the only order the host allows. */
    suspend fun start(): StartedContainers {
        val root = fixture.host.startRoot(fixture.owner("surf-api", surfApi))

        return StartedContainers(
            root = root,
            pluginA = fixture.host.startChild(fixture.owner("plugin-a", pluginA)),
            pluginB = fixture.host.startChild(fixture.owner("plugin-b", pluginB)),
        )
    }
}

/** The three containers a started topology has. */
private class StartedContainers(
    val root: BeanContainer,
    val pluginA: BeanContainer,
    val pluginB: BeanContainer,
)

/**
 * One process: a host over a platform that answers with the loaders a case built, plus the compiled
 * classes and the per-application entries those loaders are made of.
 */
private class IsolationFixture(private val temp: Path) {

    val logger = IsolationLogger()

    private val loaders = mutableMapOf<Any, ClassLoader>()

    val host = SurfContainerHost(IsolatedPlatform(loaders, logger))

    /** Where [ISOLATION_SOURCES] was compiled to; the source of every class file placed below. */
    private val compiled: Path = compileIsolationClasses(temp.resolve("javac"))

    /** A loader standing in for a server's own class path: [name]'s entry over the test's own. */
    fun server(
        name: String,
        classes: List<String> = emptyList(),
        beans: List<BeanDefinition> = emptyList(),
    ) = ServerClassLoader(entryFor(name, classes, beans))

    /** A loader with Paper's four-step delegation order over [name]'s entry. */
    fun paperPlugin(
        name: String,
        parent: ClassLoader,
        classes: List<String> = emptyList(),
        beans: List<BeanDefinition> = emptyList(),
    ) = PaperPluginClassLoader(name, entryFor(name, classes, beans), parent)

    /** A loader with Minestom's parent-first delegation order over [name]'s entry. */
    fun minestomPlugin(
        name: String,
        parent: ClassLoader,
        classes: List<String> = emptyList(),
        beans: List<BeanDefinition> = emptyList(),
    ) = MinestomPluginClassLoader(name, entryFor(name, classes, beans), parent)

    /** Registers [loader] as the platform's answer for a fresh owner named [name]. */
    fun owner(name: String, loader: ClassLoader): Any =
        Application(name).also { loaders[it] = loader }

    /**
     * Lays out one application's own class path entry: [classes] copied out of the compiled output,
     * and one metadata file declaring [beans].
     *
     * One directory serves both, exactly as a plugin jar does — which is what makes "a plugin's own
     * entry" a single thing a loader can be pointed at.
     */
    private fun entryFor(name: String, classes: List<String>, beans: List<BeanDefinition>): Path {
        val entry = Files.createDirectories(temp.resolve("applications").resolve(name))

        classes.forEach { className ->
            val relative = className.replace('.', '/') + ".class"
            val target = entry.resolve(relative)

            Files.createDirectories(target.parent)
            Files.copy(compiled.resolve(relative), target)
        }

        Files.writeString(
            Files.createDirectories(entry.resolve(BeanMetadata.DIRECTORY)).resolve("00-module.json"),
            BeanMetadata.json.encodeToString(BeanMetadata.serializer(), BeanMetadata(beans)),
        )

        return entry
    }
}

/** Answers the container's platform questions from the loaders a case registered, and nothing else. */
private class IsolatedPlatform(
    private val loaders: Map<Any, ClassLoader>,
    private val logger: IsolationLogger,
) : ContainerPlatform {

    override fun classLoader(owner: Any): ClassLoader = requireNotNull(loaders[owner]) {
        "$owner was never registered with the fixture, so it has no loader"
    }

    /**
     * The owner's own metadata.
     *
     * Every loader in this file overrides `getResources` for this one name and answers from its own
     * entry only, so a case controls exactly which files a container sees — which is also the
     * property a real platform has to provide for itself.
     */
    override fun beanSources(owner: Any): List<URL> =
        classLoader(owner).getResources(BeanMetadata.DIRECTORY).toList()

    override fun isPluginLoaded(pluginId: String) = false

    override fun logger(owner: Any): PlatformLogger = logger

    override fun dataPath(owner: Any): Path = Path.of("build", "test-data")

    override fun environments(): Set<String> = emptySet()

    override fun property(owner: Any, key: List<String>, file: String): String? = null
}

/** An application the host knows only by identity; the name is what a failure message shows. */
private class Application(private val name: String) {

    override fun toString() = name
}

/**
 * Keeps every line the host logged, by level.
 *
 * The silence of a mismatched topology is a result in its own right, so a case has to be able to
 * assert that nothing was warned about and nothing failed.
 */
private class IsolationLogger : PlatformLogger {

    val lines = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val errors = mutableListOf<String>()

    override fun info(message: String) {
        lines += message
    }

    override fun warn(message: String) {
        lines += message
        warnings += message
    }

    override fun error(message: String, throwable: Throwable?) {
        lines += if (throwable == null) message else "$message $throwable"
        errors += message
    }

    override fun debug(message: String) {
        lines += message
    }
}

// --- the loaders --------------------------------------------------------------------------------

/**
 * The resources an application serves under [BeanMetadata.DIRECTORY]: its own entry, never a
 * parent's.
 *
 * Every loader here overrides the lookup for that one name, for two reasons. The test class path
 * carries whatever metadata the rest of the repository happens to build, and inheriting it would
 * make what a case sees depend on modules it has nothing to do with. And a real plugin loader's
 * resource lookup is not the same thing as its class lookup: on Paper a plugin's loader sees its own
 * jar and its libraries and nothing else, which is what per-application bean discovery relies on,
 * while Minestom's `PluginClassLoader` inherits [ClassLoader.getResources] unchanged and is
 * therefore parent-first *and additive* for resources, even though it is parent-first and exclusive
 * for classes.
 */
private fun ownMetadataOf(entry: Path): Enumeration<URL> =
    Collections.enumeration(listOf(entry.resolve(BeanMetadata.DIRECTORY).toUri().toURL()))

/** A server's own class path: [entry] over the class path this test itself runs on. */
private class ServerClassLoader(private val entry: Path) : URLClassLoader(
    "server",
    arrayOf(entry.toUri().toURL()),
    ServerClassLoader::class.java.classLoader,
) {

    override fun getResources(name: String): Enumeration<URL> =
        if (name == BeanMetadata.DIRECTORY) ownMetadataOf(entry) else super.getResources(name)
}

/**
 * One plugin's classes, with Paper's delegation order.
 *
 * `PaperPluginClassLoader.loadClass(name, resolve, checkGlobal, checkLibraries)` tries, in order:
 *
 * 1. `super.loadClass` — a plain [URLClassLoader], parent first: the server, then this plugin's own
 *    jar
 * 2. this plugin's own [libraries] loader
 * 3. the plugin's [group], consulted with `checkGlobal = false` so the walk cannot recurse
 * 4. [ClassNotFoundException]
 *
 * The consequence the container lives with is in steps 1 and 3: **a plugin's own jar beats its
 * group**. surf-api is itself a plugin on Paper and is only ever reachable through the group, so a
 * plugin that shades a surf-api interface resolves its own copy and the shared bean it is injected
 * stops being castable. The same shaded copy on the *server's* class path would be harmless, since
 * step 1 is parent-first — the breakage is specific to surf-api being a sibling.
 */
private class PaperPluginClassLoader(
    name: String,
    private val entry: Path,
    parent: ClassLoader,
) : URLClassLoader("plugin-$name", arrayOf(entry.toUri().toURL()), parent) {

    /**
     * The loaders this plugin's own manifest lets it reach: the plugins it declares a dependency on.
     *
     * Asymmetric on purpose. A plugin that declares surf-api gets surf-api's loader here; surf-api's
     * own list stays empty, because `has-open-classloader: false` keeps its loader out of the open
     * group and nothing is ever added to it. That is what makes the root's loader unable to reach a
     * downstream plugin's classes.
     */
    @Volatile
    var group: List<PaperPluginClassLoader> = emptyList()

    /** This plugin's own library loader, consulted after its own jar and before its [group]. */
    @Volatile
    var libraries: URLClassLoader? = null

    override fun loadClass(name: String, resolve: Boolean): Class<*> =
        loadClass(name, resolve, checkGlobal = true, checkLibraries = true)

    /** Paper's own four-argument entry point; the group calls it with `checkGlobal = false`. */
    fun loadClass(
        name: String,
        resolve: Boolean,
        checkGlobal: Boolean,
        checkLibraries: Boolean,
    ): Class<*> {
        try {
            return super.loadClass(name, resolve)
        } catch (_: ClassNotFoundException) {
            // Step 1 found nothing: neither the server nor this plugin's own jar has it.
        }

        if (checkLibraries) {
            libraries?.let { loader ->
                try {
                    return loader.loadClass(name)
                } catch (_: ClassNotFoundException) {
                    // Step 2 found nothing either.
                }
            }
        }

        if (checkGlobal) {
            group.forEach { neighbour ->
                try {
                    return neighbour.loadClass(
                        name,
                        resolve,
                        checkGlobal = false,
                        checkLibraries = true,
                    )
                } catch (_: ClassNotFoundException) {
                    // This neighbour does not have it; try the next.
                }
            }
        }

        throw ClassNotFoundException(name)
    }

    override fun getResources(name: String): Enumeration<URL> =
        if (name == BeanMetadata.DIRECTORY) ownMetadataOf(entry) else super.getResources(name)

    override fun toString() = "PaperPluginClassLoader($name)"
}

/**
 * One plugin's classes, with Minestom's delegation order.
 *
 * Mirrors the production loader: [findLoadedClass], the parent, this plugin's own jar, then the jars
 * of the plugins it depends on. Parent-first all the way, which is why a type on the server's class
 * path — and surf-api is on it there — is loaded exactly once however many plugins bundle a copy of
 * it. Minestom is safe by construction, and that is the difference this file records.
 */
private class MinestomPluginClassLoader(
    name: String,
    private val entry: Path,
    parent: ClassLoader,
) : URLClassLoader("plugin-$name", arrayOf(entry.toUri().toURL()), parent) {

    /** The loaders of the plugins this one depends on, consulted last and own-entry-only. */
    @Volatile
    var dependencies: List<MinestomPluginClassLoader> = emptyList()

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            val loaded = findLoadedClass(name)
                ?: fromParent(name)
                ?: fromOwnEntry(name)
                ?: dependencies.firstNotNullOfOrNull { it.findOwnClass(name) }
                ?: throw ClassNotFoundException(name)

            if (resolve) resolveClass(loaded)

            return loaded
        }
    }

    /** The class from this plugin's own entry, without consulting the server or other plugins. */
    fun findOwnClass(name: String): Class<*>? = synchronized(getClassLoadingLock(name)) {
        findLoadedClass(name) ?: fromOwnEntry(name)
    }

    override fun getResources(name: String): Enumeration<URL> =
        if (name == BeanMetadata.DIRECTORY) ownMetadataOf(entry) else super.getResources(name)

    override fun toString() = "MinestomPluginClassLoader($name)"

    private fun fromParent(name: String): Class<*>? = try {
        parent.loadClass(name)
    } catch (_: ClassNotFoundException) {
        null
    }

    private fun fromOwnEntry(name: String): Class<*>? = try {
        findClass(name)
    } catch (_: ClassNotFoundException) {
        null
    }
}

// --- the classes the topologies are built from --------------------------------------------------

/** The interface a shared bean is typed by, whose class identity is what every case asserts on. */
private const val GREETING = "isolation.Greeting"

/** The implementation surf-api publishes; only ever loaded by the loader that owns [GREETING]. */
private const val GREETING_IMPL = "isolation.GreetingImpl"

/** A plugin's own compiled code, which performs the cast the container cannot perform for it. */
private const val CONSUMER = "isolation.Consumer"

/** What a successful cast gets you, so a case can tell a real call from a silent no-op. */
private const val GREETING_TEXT = "the one shared greeting"

/**
 * The Java sources compiled during the test, in compilation order.
 *
 * Java rather than Kotlin because the JDK ships its own compiler and the test runtime has no Kotlin
 * compiler to call. None of these classes is ever built *by* the container — see
 * [publisherDefinition] for why — so none of them needs Kotlin metadata.
 */
private val ISOLATION_SOURCES = linkedMapOf(
    "Greeting.java" to """
        package isolation;

        /** The type a shared bean crosses an application boundary as. */
        public interface Greeting {
            String greet();
        }
    """.trimIndent(),
    "GreetingImpl.java" to """
        package isolation;

        /** The one implementation, loaded by whichever loader owns the interface. */
        public final class GreetingImpl implements Greeting {
            @Override
            public String greet() {
                return "$GREETING_TEXT";
            }
        }
    """.trimIndent(),
    "Consumer.java" to """
        package isolation;

        /** A plugin's own code, compiled against whichever Greeting its loader resolves. */
        public final class Consumer {

            private Consumer() {
            }

            public static String consume(Object bean) {
                return ((Greeting) bean).greet();
            }
        }
    """.trimIndent(),
)

/**
 * Compiles [ISOLATION_SOURCES] under [into] and returns the directory the class files landed in.
 *
 * The JDK compiler is used rather than checked-in class files so the bytecode always matches the JDK
 * the tests run on. It is part of every JDK; a test runtime without it would be a JRE, which
 * Gradle's toolchain does not produce.
 */
private fun compileIsolationClasses(into: Path): Path {
    val compiler = assertNotNull(
        ToolProvider.getSystemJavaCompiler(),
        "This test needs the JDK compiler: two distinct Class objects with the same binary name " +
                "cannot be produced without compiling the same interface twice.",
    )

    val sources = Files.createDirectories(into.resolve("sources").resolve("isolation"))
    val classes = Files.createDirectories(into.resolve("classes"))

    ISOLATION_SOURCES.forEach { (file, text) -> Files.writeString(sources.resolve(file), text) }

    val diagnostics = ByteArrayOutputStream()
    val status = compiler.run(
        null,
        null,
        diagnostics,
        "-d",
        classes.toString(),
        *ISOLATION_SOURCES.keys.map { sources.resolve(it).toString() }.toTypedArray(),
    )

    assertEquals(0, status, "javac failed to compile the isolation classes: $diagnostics")

    return classes
}

/** A fresh [GREETING_IMPL] as [loader] defines it, which is the instance a case shares. */
private fun newGreeting(loader: ClassLoader): Any =
    loader.loadClass(GREETING_IMPL).getDeclaredConstructor().newInstance()

/**
 * Hands [bean] to [CONSUMER] as [loader] defines it, and returns what the cast produced.
 *
 * The reflective call's wrapper is unwrapped, because the failure a case asserts on is the one the
 * plugin's own bytecode raised and not the fact that it was reached by reflection.
 */
private fun consume(loader: ClassLoader, bean: Any): String = try {
    loader.loadClass(CONSUMER).getMethod("consume", Any::class.java).invoke(null, bean) as String
} catch (failure: InvocationTargetException) {
    throw failure.targetException
}

// --- the beans the cases declare ----------------------------------------------------------------

/**
 * The instance the root's factory publishes, set by each case before the root is started.
 *
 * It lives outside [GreetingPublisher] because the object has to be created by a loader the case
 * built, and the publisher's own class is loaded from the test class path, which cannot see the
 * interface at all.
 */
private var publishedGreeting: Any? = null

/**
 * surf-api's own bean, whose `@Bean` property publishes the shared instance.
 *
 * A factory rather than an annotated class, because the shared bean's class is compiled during the
 * test and [dev.slne.surf.api.di.internal.ReflectiveBeanInstantiator] cannot build a class that
 * carries no Kotlin metadata — it reads the *primary* constructor, which a Java class does not have.
 * A factory is the container's own answer for publishing a type it cannot construct itself, so the
 * path under test is a real one.
 */
internal class GreetingPublisher {

    val greeting: Any
        get() = checkNotNull(publishedGreeting) {
            "The case has to publish an instance before the root is started."
        }
}

/**
 * Plugin A's own bean.
 *
 * The parameter is an [Any] because the interface it is really given does not exist when this file
 * is compiled — which is the point. What the plugin would have written as a typed parameter is
 * asserted instead, against the interface as the plugin's own loader sees it, and performed for real
 * by [CONSUMER].
 */
internal class PluginABean(val greeting: Any)

/** Plugin B's own bean, declared separately so a case can tell the two applications apart. */
internal class PluginBBean(val greeting: Any)

/** The metadata for [GreetingPublisher] and the [Scope.SHARED] bean its property publishes. */
private fun publisherDefinition() = BeanDefinition(
    className = GreetingPublisher::class.java.name,
    provides = listOf(GreetingPublisher::class.java.name),
    scope = Scope.SHARED,
    factories = listOf(
        FactoryDefinition(
            memberName = "greeting",
            kind = FactoryKind.PROPERTY,
            returnType = GREETING,
            provides = listOf(GREETING),
        ),
    ),
)

/** The metadata for a plugin's own bean, which takes the shared interface by its binary name. */
private fun pluginBeanDefinition(type: Class<*>) = BeanDefinition(
    className = type.name,
    provides = listOf(type.name),
    ctorParams = listOf(InjectionPoint(type = GREETING)),
)
