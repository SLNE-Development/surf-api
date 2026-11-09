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

            fixture.host.stop(plugin)

            assertNull(fixture.host.containerOf(plugin))

            val second = fixture.host.startChild(plugin)

            assertNotSame(first, second, "the owner gets a fresh container")
            assertSame(second, fixture.host.containerOf(plugin))
            assertEquals(
                listOf("child load", "child enable", "child disable", "child load", "child enable"),
                phases,
            )
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
        val root = temp.resolve(name)
        val directory = Files.createDirectories(root.resolve(BeanMetadata.DIRECTORY))

        files.forEachIndexed { index, beans ->
            Files.writeString(
                directory.resolve("%02d-module.json".format(index)),
                BeanMetadata.json.encodeToString(BeanMetadata.serializer(), BeanMetadata(beans)),
            )
        }

        classLoaders[owner] = MetadataClassLoader(root)

        return owner
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

    private val lines = mutableListOf<String>()

    override fun info(message: String) {
        lines += message
    }

    override fun warn(message: String) {
        lines += message
    }

    override fun error(message: String, throwable: Throwable?) {
        lines += if (throwable == null) message else "$message $throwable"
    }

    override fun debug(message: String) {
        lines += message
    }

    /** The single line mentioning [needle], which is how a case reads what the host reported. */
    fun lineMentioning(needle: String): String = assertNotNull(
        lines.singleOrNull { needle in it },
        "expected exactly one logged line mentioning $needle, but logged: $lines",
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

/** The phases the lifecycle beans ran, in order; cleared before every case. */
private val phases = mutableListOf<String>()

/** A class name nothing on the test classpath answers to. */
private const val ABSENT_CLASS = "does.not.Exist"

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

/** A root-owned component that records its phases. */
internal class RootComponent : SurfComponent {

    override suspend fun load() {
        phases += "root load"
    }

    override suspend fun enable() {
        phases += "root enable"
    }

    override suspend fun disable() {
        phases += "root disable"
    }
}

/** A child-owned component that records its phases. */
internal class ChildComponent : SurfComponent {

    override suspend fun load() {
        phases += "child load"
    }

    override suspend fun enable() {
        phases += "child enable"
    }

    override suspend fun disable() {
        phases += "child disable"
    }
}

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
