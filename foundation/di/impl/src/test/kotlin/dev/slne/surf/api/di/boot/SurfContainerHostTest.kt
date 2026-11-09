package dev.slne.surf.api.di.boot

import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.PlatformLogger
import dev.slne.surf.api.di.SurfComponent
import dev.slne.surf.api.di.condition.BeanCondition
import dev.slne.surf.api.di.condition.BeanConditionContext
import dev.slne.surf.api.di.definition.BeanConditions
import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.definition.BeanMetadata
import dev.slne.surf.api.di.definition.InjectionPoint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.Enumeration
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Exercises the host end to end: real metadata files, real bean classes, real phases.
 *
 * The other container tests hand definitions straight to the container and stub the instantiator,
 * which is what keeps them about ordering and resolution. The host is the part that has to put the
 * real pieces together — read the classpath, judge the conditions, build by reflection, remember
 * the result — so every case here writes metadata into a temporary directory and names classes that
 * actually exist in this file. A stub anywhere in that chain would test the test.
 */
class SurfContainerHostTest {

    @BeforeTest
    fun forgetRecordedPhases() = phases.clear()

    // --- starting the root ----------------------------------------------------------------------

    @Test
    fun `startRoot builds a started container and publishes it as the root`(@TempDir temp: Path) =
        runTest {
            val fixture = Fixture(temp)
            val api = fixture.owner(
                "surf-api",
                listOf(
                    definitionOf(Standalone::class),
                    definitionOf(RootComponent::class, lifecycle = true),
                ),
            )

            assertNull(fixture.host.root, "there is no root before startRoot")

            val root = fixture.host.startRoot(api)

            assertNotNull(root.beanOrNull(Standalone::class), "the root's beans are built")

            // startRoot runs `load` only; `enable` is a second call, so a bean's enable lands in
            // the platform's own enable phase rather than in its load phase.
            assertEquals(listOf("root load"), phases)

            fixture.host.enable(api)

            assertEquals(listOf("root load", "root enable"), phases)
            assertSame(root, fixture.host.root)
            assertSame(api, root.owner)
        }

    @Test
    fun `a second startRoot fails and names the owner of the existing root`(@TempDir temp: Path) =
        runTest {
            val fixture = Fixture(temp)
            val api = fixture.owner("surf-api")
            val impostor = fixture.owner("another-root")

            fixture.host.startRoot(api)

            val failure = assertFailsWith<IllegalStateException> { fixture.host.startRoot(impostor) }
            val message = failure.message.orEmpty()

            assertContains(message, "surf-api")
            assertContains(message, "startChild")
        }

    // --- starting a child -----------------------------------------------------------------------

    @Test
    fun `startChild before startRoot fails and points at startRoot`(@TempDir temp: Path) = runTest {
        val fixture = Fixture(temp)
        val plugin = fixture.owner("plugin", listOf(definitionOf(Standalone::class)))

        val failure = assertFailsWith<IllegalStateException> { fixture.host.startChild(plugin) }

        assertContains(failure.message.orEmpty(), "startRoot")
        assertNull(fixture.host.containerOf(plugin), "a refused child leaves nothing behind")
    }

    @Test
    fun `a second startChild for one owner fails and warns about two instances`(@TempDir temp: Path) =
        runTest {
            val fixture = Fixture(temp)
            val api = fixture.owner("surf-api")
            val plugin = fixture.owner("plugin", listOf(definitionOf(Standalone::class)))

            fixture.host.startRoot(api)
            val first = fixture.host.startChild(plugin)

            val failure = assertFailsWith<IllegalStateException> { fixture.host.startChild(plugin) }
            val message = failure.message.orEmpty()

            assertContains(message, "two instances of every bean")
            assertContains(message, "containerOf")
            assertSame(first, fixture.host.containerOf(plugin), "the first container still stands")
        }

    @Test
    fun `a child resolves a bean the root provides`(@TempDir temp: Path) = runTest {
        val fixture = Fixture(temp)
        val api = fixture.owner("surf-api", listOf(definitionOf(Standalone::class)))
        val plugin = fixture.owner(
            "plugin",
            listOf(definitionOf(Dependent::class, needs = listOf(pointTo(Standalone::class)))),
        )

        val root = fixture.host.startRoot(api)
        val child = fixture.host.startChild(plugin)
        val shared = root.bean(Standalone::class)

        assertSame(root, child.parent)
        assertSame(shared, child.bean(Standalone::class), "a lookup walks up to the root")
        assertSame(shared, child.bean(Dependent::class).dependency, "so does an injected parameter")
    }

    // --- remembering containers -----------------------------------------------------------------

    @Test
    fun `containerOf answers for a started owner and nothing else`(@TempDir temp: Path) = runTest {
        val fixture = Fixture(temp)
        val api = fixture.owner("surf-api")
        val plugin = fixture.owner("plugin", listOf(definitionOf(Standalone::class)))
        val stranger = Owner("never-started")

        val root = fixture.host.startRoot(api)

        assertNull(fixture.host.containerOf(plugin), "a child is unknown until it is started")

        val child = fixture.host.startChild(plugin)

        assertSame(child, fixture.host.containerOf(plugin))
        assertSame(root, fixture.host.containerOf(api), "the root is remembered under its owner too")
        assertNull(fixture.host.containerOf(stranger))
    }

    // --- conditions, before the container exists ------------------------------------------------

    @Test
    fun `a bean whose condition cannot hold is never in the container and is reported`(
        @TempDir temp: Path,
    ) = runTest {
        val fixture = Fixture(temp)
        val api = fixture.owner(
            "surf-api",
            listOf(
                definitionOf(Standalone::class),
                definitionOf(
                    Gated::class,
                    conditions = BeanConditions(classNames = listOf(ABSENT_CLASS)),
                ),
            ),
        )

        val root = fixture.host.startRoot(api)

        assertNull(root.beanOrNull(Gated::class), "a rejected bean must not be resolvable")
        assertNotNull(root.beanOrNull(Standalone::class), "its neighbour is unaffected")

        val report = fixture.logger.lineMentioning(Gated::class.java.name)

        assertContains(report, ABSENT_CLASS)
        assertContains(report, "not on the classpath")
    }

    @Test
    fun `a missing-bean condition is answered over everything declared, in either file order`(
        @TempDir temp: Path,
    ) = runTest {
        val fallback = definitionOf(
            DefaultStorage::class,
            provides = listOf(DefaultStorage::class.java.name, Storage::class.java.name),
            conditions = BeanConditions(missingBeans = listOf(Storage::class.java.name)),
        )
        val real = definitionOf(
            RealStorage::class,
            provides = listOf(RealStorage::class.java.name, Storage::class.java.name),
        )

        val fallbackFirst = soleStorageIn(temp.resolve("fallback-first"), fallback, real)
        val realFirst = soleStorageIn(temp.resolve("real-first"), real, fallback)

        assertEquals(RealStorage::class.java.name, fallbackFirst)
        assertEquals(fallbackFirst, realFirst, "the file order must not decide")
    }

    @Test
    fun `a condition that resolves a bean fails with the late-container message`(@TempDir temp: Path) =
        runTest {
            val fixture = Fixture(temp)
            val api = fixture.owner(
                "surf-api",
                listOf(
                    definitionOf(Standalone::class),
                    definitionOf(
                        Gated::class,
                        conditions = BeanConditions(
                            customConditions = listOf(ResolvesABean::class.java.name),
                        ),
                    ),
                ),
            )

            val root = fixture.host.startRoot(api)

            assertNull(root.beanOrNull(Gated::class))

            val report = fixture.logger.lineMentioning(Gated::class.java.name)

            assertContains(report, ResolvesABean::class.java.name)
            assertContains(report, "while the container was still being built")
            assertFalse(
                report.contains("UninitializedPropertyAccessException"),
                "the handle must explain itself instead of leaking an uninitialised lateinit",
            )
        }

    // --- a start that fails ---------------------------------------------------------------------

    @Test
    fun `a root whose beans fail to build is not published, and the retry succeeds`(
        @TempDir temp: Path,
    ) = runTest {
        val fixture = Fixture(temp)
        val api = fixture.owner("surf-api", listOf(definitionOf(Unbuildable::class)))

        val failure = assertFails { fixture.host.startRoot(api) }

        assertContains(messagesOf(failure), CONSTRUCTION_FAILURE)
        assertNull(fixture.host.root, "a container whose beans failed must not become the root")
        assertNull(fixture.host.containerOf(api))

        fixture.serve(
            api,
            listOf(
                definitionOf(Standalone::class),
                definitionOf(RootComponent::class, lifecycle = true),
            ),
        )

        val root = fixture.host.startRoot(api)
        fixture.host.enable(api)

        assertSame(root, fixture.host.root, "the failed attempt did not use up the one root")
        assertSame(root, fixture.host.containerOf(api))
        assertNotNull(root.beanOrNull(Standalone::class))
        assertEquals(listOf("root load", "root enable"), phases)
    }

    @Test
    fun `a child whose beans fail to build leaves no entry, and the retry succeeds`(
        @TempDir temp: Path,
    ) = runTest {
        val fixture = Fixture(temp)
        val api = fixture.owner("surf-api")
        val plugin = fixture.owner("plugin", listOf(definitionOf(Unbuildable::class)))

        fixture.host.startRoot(api)

        val failure = assertFails { fixture.host.startChild(plugin) }

        assertContains(messagesOf(failure), CONSTRUCTION_FAILURE)
        assertNull(fixture.host.containerOf(plugin), "a failed child must not be handed out")

        fixture.serve(plugin, listOf(definitionOf(ChildComponent::class, lifecycle = true)))

        val child = fixture.host.startChild(plugin)
        fixture.host.enable(plugin)

        assertSame(child, fixture.host.containerOf(plugin), "the owner was free to start again")
        assertEquals(listOf("child load", "child enable"), phases)
    }

    // --- stopping -------------------------------------------------------------------------------

    @Test
    fun `stop forgets a child, so the same owner can be started again`(@TempDir temp: Path) =
        runTest {
            val fixture = Fixture(temp)
            val api = fixture.owner("surf-api")
            val plugin = fixture.owner(
                "plugin",
                listOf(definitionOf(ChildComponent::class, lifecycle = true)),
            )

            fixture.host.startRoot(api)
            val first = fixture.host.startChild(plugin)
            fixture.host.enable(plugin)

            fixture.host.stop(plugin)

            assertNull(fixture.host.containerOf(plugin))

            val second = fixture.host.startChild(plugin)
            fixture.host.enable(plugin)

            assertNotSame(first, second, "the owner gets a fresh container")
            assertSame(second, fixture.host.containerOf(plugin))
            assertEquals(
                listOf("child load", "child enable", "child disable", "child load", "child enable"),
                phases,
            )
        }

    @Test
    fun `stopping the root while a child runs is refused and tears nothing down`(
        @TempDir temp: Path,
    ) = runTest {
        val fixture = Fixture(temp)
        val api = fixture.owner(
            "surf-api",
            listOf(
                definitionOf(Standalone::class),
                definitionOf(RootComponent::class, lifecycle = true),
            ),
        )
        val plugin = fixture.owner(
            "plugin",
            listOf(definitionOf(ChildComponent::class, lifecycle = true)),
        )

        val root = fixture.host.startRoot(api)
        val child = fixture.host.startChild(plugin)

        val failure = assertFailsWith<IllegalStateException> { fixture.host.stop(api) }
        val message = failure.message.orEmpty()

        assertContains(message, "plugin")
        assertContains(message, "stopAll")

        // The check has to come before anything is torn down: a guard that fires after the fact
        // would leave the process in the state it was refusing to produce.
        assertEquals(
            emptyList<String>(),
            phases.filter { it.endsWith("disable") },
            "a refused stop must not disable anything",
        )
        assertSame(root, fixture.host.root)
        assertSame(root, fixture.host.containerOf(api))
        assertSame(child, fixture.host.containerOf(plugin))
        assertNotNull(root.beanOrNull(Standalone::class), "the root still answers")
        assertNotNull(child.beanOrNull(ChildComponent::class), "so does the child")
    }

    @Test
    fun `the root can be stopped once its last child is gone`(@TempDir temp: Path) = runTest {
        val fixture = Fixture(temp)
        val api = fixture.owner("surf-api", listOf(definitionOf(RootComponent::class, lifecycle = true)))
        val plugin = fixture.owner(
            "plugin",
            listOf(definitionOf(ChildComponent::class, lifecycle = true)),
        )

        fixture.host.startRoot(api)
        fixture.host.startChild(plugin)

        fixture.host.stop(plugin)
        fixture.host.stop(api)

        assertEquals(listOf("child disable", "root disable"), phases.filter { it.endsWith("disable") })
        assertNull(fixture.host.root)
        assertNull(fixture.host.containerOf(api))
    }

    @Test
    fun `stopAll stops the siblings youngest first and the root last`(@TempDir temp: Path) =
        runTest {
            val fixture = Fixture(temp)
            val api = fixture.owner(
                "surf-api",
                listOf(definitionOf(RootComponent::class, lifecycle = true)),
            )
            val first = fixture.owner(
                "first-plugin",
                listOf(definitionOf(FirstChildComponent::class, lifecycle = true)),
            )
            val second = fixture.owner(
                "second-plugin",
                listOf(definitionOf(SecondChildComponent::class, lifecycle = true)),
            )
            val third = fixture.owner(
                "third-plugin",
                listOf(definitionOf(ThirdChildComponent::class, lifecycle = true)),
            )

            fixture.host.startRoot(api)
            fixture.host.startChild(first)
            fixture.host.startChild(second)
            fixture.host.startChild(third)

            fixture.host.stopAll()

            assertEquals(
                listOf("third disable", "second disable", "first disable", "root disable"),
                phases.filter { it.endsWith("disable") },
                "the last application started is the one most likely to hold the others",
            )
            assertNull(fixture.host.root)
            listOf(api, first, second, third).forEach { assertNull(fixture.host.containerOf(it)) }
        }

    @Test
    fun `stopAll disables the children before the root`(@TempDir temp: Path) = runTest {
        val fixture = Fixture(temp)
        val api = fixture.owner("surf-api", listOf(definitionOf(RootComponent::class, lifecycle = true)))
        val plugin = fixture.owner(
            "plugin",
            listOf(definitionOf(ChildComponent::class, lifecycle = true)),
        )

        fixture.host.startRoot(api)
        fixture.host.startChild(plugin)
        fixture.host.stopAll()

        assertEquals(
            listOf("child disable", "root disable"),
            phases.filter { it.endsWith("disable") },
            "a child's bean may hold the root's, so the child goes down first",
        )
        assertNull(fixture.host.root)
        assertNull(fixture.host.containerOf(api))
        assertNull(fixture.host.containerOf(plugin))
    }

    // --- helpers --------------------------------------------------------------------------------

    /**
     * The class name of the one bean providing [Storage] in a fresh process over [files].
     *
     * One host per call, each with its own directory, so the two orders really are two independent
     * builds rather than the same container asked twice.
     */
    private suspend fun soleStorageIn(temp: Path, vararg files: BeanDefinition): String {
        val fixture = Fixture(Files.createDirectories(temp))
        val api = fixture.owner("surf-api", *files.map { listOf(it) }.toTypedArray())

        val root = fixture.host.startRoot(api)
        val beans = root.beansOfType(Storage::class)

        return assertNotNull(
            beans.singleOrNull(),
            "exactly one bean has to provide Storage, but found ${beans.map { it::class.java.name }}",
        )::class.java.name
    }
}

// --- fixtures -----------------------------------------------------------------------------------

/**
 * One process: a host, and a platform that serves every owner exactly the metadata a case wrote.
 *
 * Owners are registered before they are started, which is what lets a case give the root and a
 * child different beans — on a real platform the two live behind different classloaders, and that
 * is the only reason they are two containers.
 */
private class Fixture(private val temp: Path) {

    val logger = RecordingLogger()

    private val classLoaders = mutableMapOf<Any, ClassLoader>()

    val host = SurfContainerHost(FakePlatform(classLoaders, logger))

    /**
     * Registers an owner named [name] whose classpath carries one metadata file per entry of
     * [files], in that order.
     *
     * The file names count: the loader reads a directory in name order, so numbering them is how a
     * case decides which module's definitions arrive first.
     */
    fun owner(name: String, vararg files: List<BeanDefinition>): Any {
        val owner = Owner(name)

        classLoaders[owner] = MetadataClassLoader(temp.resolve(name))
        serve(owner, *files)

        return owner
    }

    /**
     * Replaces what [owner]'s classpath carries.
     *
     * This is how a case gives a retry a working set of beans: the owner is the same object the
     * platform is asked about, and the host reads the directory again on every build.
     */
    fun serve(owner: Any, vararg files: List<BeanDefinition>) {
        val directory = Files.createDirectories(
            temp.resolve(owner.toString()).resolve(BeanMetadata.DIRECTORY)
        )

        Files.newDirectoryStream(directory).use { existing -> existing.forEach(Files::delete) }

        files.forEachIndexed { index, beans ->
            Files.writeString(
                directory.resolve("%02d-module.json".format(index)),
                BeanMetadata.json.encodeToString(BeanMetadata.serializer(), BeanMetadata(beans)),
            )
        }
    }
}

/** An application the host knows only by identity; the name is what a failure message shows. */
private class Owner(private val name: String) {

    override fun toString() = name
}

/** Answers the platform's questions from what the fixture registered, and nothing else. */
private class FakePlatform(
    private val classLoaders: Map<Any, ClassLoader>,
    private val logger: RecordingLogger,
) : ContainerPlatform {

    override fun classLoader(owner: Any): ClassLoader = requireNotNull(classLoaders[owner]) {
        "$owner was never registered with the fixture, so it has no metadata"
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

    override fun dataPath(owner: Any): Path = DATA_PATH

    override fun environments(): Set<String> = emptySet()

    override fun property(owner: Any, key: List<String>, file: String): String? = null
}

/**
 * Serves one owner's metadata directory while loading classes from the test's own loader.
 *
 * The host reads metadata *and* builds the beans by reflection, so its loader has to answer for the
 * bean classes declared in this file while serving only the resources one case wrote. An isolated
 * [java.net.URLClassLoader] does the second but not the first, and inheriting the test classpath's
 * own metadata would make what a case sees depend on the rest of the repository — hence exactly one
 * overridden resource name.
 */
private class MetadataClassLoader(private val root: Path) :
    ClassLoader(MetadataClassLoader::class.java.classLoader) {

    override fun getResources(name: String): Enumeration<URL> =
        if (name == BeanMetadata.DIRECTORY) {
            Collections.enumeration(listOf(root.resolve(name).toUri().toURL()))
        } else {
            super.getResources(name)
        }
}

/**
 * Keeps every line the host logged.
 *
 * A rejected bean is only ever reported, never returned, so the report is the whole observable
 * behaviour of a condition that does not hold — a silent logger would leave that untested.
 */
private class RecordingLogger : PlatformLogger {

    private val lines = mutableListOf<Pair<String, String>>()

    override fun info(message: String) {
        lines += "info" to message
    }

    override fun warn(message: String) {
        lines += "warn" to message
    }

    override fun error(message: String, throwable: Throwable?) {
        lines += "error" to if (throwable == null) message else "$message $throwable"
    }

    override fun debug(message: String) {
        lines += "debug" to message
    }

    /**
     * The single [level] line mentioning [needle], which is how a case reads what the host
     * reported.
     *
     * The level is part of the question because the host says the same thing twice at two levels:
     * the reason a bean was skipped at `debug`, and a count of what was skipped at `info`, so that
     * a server with debug logging off still shows that something is missing. A case that did not
     * name a level would match both.
     */
    fun lineMentioning(needle: String, level: String = "debug"): String = assertNotNull(
        lines.singleOrNull { (at, line) -> at == level && needle in line }?.second,
        "expected exactly one $level line mentioning $needle, but logged: $lines",
    )
}

/** The definition of a class that really exists, since the host builds it by reflection. */
private fun definitionOf(
    type: KClass<*>,
    provides: List<String> = listOf(type.java.name),
    needs: List<InjectionPoint> = emptyList(),
    lifecycle: Boolean = false,
    conditions: BeanConditions = BeanConditions(),
) = BeanDefinition(
    className = type.java.name,
    provides = provides,
    lifecycle = lifecycle,
    ctorParams = needs,
    conditions = conditions,
)

/** An injection point taking the one bean providing [type]. */
private fun pointTo(type: KClass<*>) = InjectionPoint(type = type.java.name)

/**
 * A failure and its causes as one string.
 *
 * A constructor that throws reaches the caller through reflection, which is free to wrap it, so a
 * case asks whether the original failure is anywhere in the chain rather than guessing the shape.
 */
private fun messagesOf(failure: Throwable): String =
    generateSequence(failure) { it.cause.takeIf { cause -> cause !== it } }.joinToString()

/** The phases the lifecycle beans ran, in order; cleared before every case. */
private val phases = mutableListOf<String>()

/** A class name nothing on the test classpath answers to. */
private const val ABSENT_CLASS = "does.not.Exist"

/** What [Unbuildable] throws, looked for in the failure a start hands back. */
private const val CONSTRUCTION_FAILURE = "this bean cannot be built"

/** Where the fake platform would keep configuration; nothing here reads a file. */
private val DATA_PATH: Path = Path.of("build", "test-data")

// --- beans the cases declare ---------------------------------------------------------------------

/** A bean with nothing to inject, the simplest thing the host can build. */
internal class Standalone

/** A bean the container has to hand another bean, so a real argument has to be resolved. */
internal class Dependent(val dependency: Standalone)

/** A bean the condition cases guard; it is only ever asked for, never used. */
internal class Gated

/** What the two beans of the missing-bean case are both resolved as. */
internal interface Storage

/** The fallback, guarded on nothing else providing [Storage]. */
internal class DefaultStorage : Storage

/** The implementation that is meant to take over from [DefaultStorage]. */
internal class RealStorage : Storage

/** A bean whose construction fails, so a case can watch what a failed start leaves behind. */
internal class Unbuildable {

    init {
        error(CONSTRUCTION_FAILURE)
    }
}

/**
 * Records every phase it runs under its own label.
 *
 * Teardown order is only readable if the beans can be told apart, and two containers holding the
 * same class hold two indistinguishable instances — hence one subclass per place a case puts one.
 */
internal abstract class PhaseRecorder(private val label: String) : SurfComponent {

    override suspend fun load() {
        phases += "$label load"
    }

    override suspend fun enable() {
        phases += "$label enable"
    }

    override suspend fun disable() {
        phases += "$label disable"
    }
}

/** The root's component. */
internal class RootComponent : PhaseRecorder("root")

/** The component of the single child in the cases that have one. */
internal class ChildComponent : PhaseRecorder("child")

/** The oldest of the three siblings, and therefore the last to be stopped. */
internal class FirstChildComponent : PhaseRecorder("first")

/** The middle sibling. */
internal class SecondChildComponent : PhaseRecorder("second")

/** The youngest sibling, and therefore the first to be stopped. */
internal class ThirdChildComponent : PhaseRecorder("third")

/**
 * Asks the container for a bean while deciding, which is the one thing a condition may not do.
 *
 * A condition decides whether a bean exists, so it runs before any bean does; the container it is
 * handed is the one still being built.
 */
internal class ResolvesABean : BeanCondition {

    override suspend fun evaluate(context: BeanConditionContext): Boolean {
        context.container.bean(Standalone::class)

        return true
    }
}
