package dev.slne.surf.api.di.boot

import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.PlatformLogger
import dev.slne.surf.api.di.SurfContainer
import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.definition.BeanMetadata
import dev.slne.surf.api.di.internal.definition
import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.di.surfBeans
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.Enumeration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Exercises the seam between an api module and the running container.
 *
 * `surfBean` is what every `requiredService` call site became, and the property it builds is read
 * from places with no constructor to inject into — a `companion object`, a top-level property, an
 * extension function. The one behaviour those places depend on is *when* it resolves: a property
 * that resolved while its class was being loaded would make the order of unrelated class loads
 * decide whether the container was reachable, which is the failure mode the old service lookup
 * had. Every case here is about that moment, so each one declares the property, then says what
 * must and must not have happened yet.
 *
 * [SurfContainerAccess] is process-wide state, so each case installs its own host and takes it
 * back out again in [uninstallHost]. A case that left one installed would decide the next one's
 * answer.
 */
class SurfContainerSeamTest {

    private var installed: SurfContainerHost? = null

    @AfterTest
    fun uninstallHost() {
        installed?.let(SurfContainerAccess::uninstall)
        installed = null
    }

    // --- when the property resolves -------------------------------------------------------------

    @Test
    fun `declaring a bean property resolves nothing`() {
        // No container is installed, and no bean of this type exists anywhere. Both reads below
        // would fail if the delegate resolved on construction rather than on first access.
        val holder = Holder()

        assertTrue(installed == null, "this case installs no host")

        // Reading the property is the first thing that may fail, and it has to fail clearly.
        val failure = assertFailsWith<IllegalStateException> { holder.greeter }

        assertContains(failure.message.orEmpty(), "has not been started yet")
    }

    @Test
    fun `an unstarted container names the fix rather than the mechanism`() {
        val failure = assertFailsWith<IllegalStateException> { SurfContainer.root }
        val message = failure.message.orEmpty()

        // The message is read by whoever wrote the class that asked too early, so it has to say
        // what to do — take the bean as a parameter or as a Lazy — not merely that the root is null.
        assertContains(message, "constructor parameter")
        assertContains(message, "Lazy")
    }

    @Test
    fun `rootOrNull answers null while surf-api is still loading`() {
        assertNull(SurfContainer.rootOrNull, "nothing is installed, so there is no root")
    }

    // --- a class initialiser that asks the container back ----------------------------------------

    @Test
    fun `a bean whose class initialiser asks for a bean is named as a cycle`(@TempDir temp: Path) =
        runTest {
            val fixture = SeamFixture(temp)
            val api = fixture.owner("surf-api", listOf(definition(EagerGreeter::class.java.name)))

            install(fixture.host)

            // Creating the bean initialises its class, whose initialiser reads a `surfBean`
            // property. Nothing can answer that: the container is part-way through creating the
            // very bean being asked for. The JVM wraps a failed initialiser, so the assertion is
            // on the whole chain — what matters is that it says the container was being built
            // rather than reading as "surf-api has not started", which is the other, unrelated
            // cause.
            val failure = assertFails { fixture.host.startRoot(api) }
            val message = failure.stackTraceToString()

            assertContains(message, "still being built")
            assertContains(message, "surf-api", ignoreCase = false)
            assertContains(message, "companion object")
        }

    @Test
    fun `a failed build stops claiming to be busy`(@TempDir temp: Path) = runTest {
        val fixture = SeamFixture(temp)
        // Its own eager class, not the one the case above uses: the JVM remembers a class whose
        // initialiser threw and answers every later use with NoClassDefFoundError, so two cases
        // sharing one would not be testing the same thing.
        val api = fixture.owner("surf-api", listOf(definition(SecondEagerGreeter::class.java.name)))

        install(fixture.host)
        assertFails { fixture.host.startRoot(api) }

        // The owner has to be forgotten on the way out, or every later failure in the process would
        // be explained as a cycle in a build that is long over.
        assertNull(fixture.host.starting, "the host is no longer building anything")

        val failure = assertFailsWith<IllegalStateException> { SurfContainer.root }

        assertContains(failure.message.orEmpty(), "has not been started yet")
    }

    // --- once the root is running ---------------------------------------------------------------

    @Test
    fun `a property resolves from the root on first access`(@TempDir temp: Path) = runTest {
        val fixture = SeamFixture(temp)
        val api = fixture.owner("surf-api", listOf(definition(Greeter::class.java.name)))

        val holder = Holder()

        install(fixture.host)
        fixture.host.startRoot(api)

        assertEquals("hello", holder.greeter.greet())
        assertSame(
            SurfContainer.root.bean(Greeter::class),
            holder.greeter,
            "the property holds the root's singleton, not a copy of it",
        )
    }

    @Test
    fun `a property resolved once keeps its value after the host is gone`(@TempDir temp: Path) =
        runTest {
            val fixture = SeamFixture(temp)
            val api = fixture.owner("surf-api", listOf(definition(Greeter::class.java.name)))

            install(fixture.host)
            fixture.host.startRoot(api)

            val holder = Holder()
            val first = holder.greeter

            fixture.host.stopAll()
            SurfContainerAccess.uninstall(fixture.host)
            installed = null

            // A `Lazy` caches, so a call site that already resolved goes on working through a
            // shutdown instead of failing halfway through one. That is deliberate: disable order is
            // the container's business, and a bean that disappeared mid-shutdown would turn an
            // orderly stop into a crash.
            assertSame(first, holder.greeter)
            assertNull(SurfContainer.rootOrNull, "the host really is uninstalled")
        }

    @Test
    fun `surfBeans answers with every bean of a type`(@TempDir temp: Path) = runTest {
        val fixture = SeamFixture(temp)
        val api = fixture.owner(
            "surf-api",
            listOf(
                definition(LoudGreeter::class.java.name, provides = greeterTypes(LoudGreeter::class.java.name)),
                definition(Greeter::class.java.name),
            ),
        )

        val holder = Holder()

        install(fixture.host)
        fixture.host.startRoot(api)

        assertEquals(
            setOf("hello", "HELLO"),
            holder.greeters.map { it.greet() }.toSet(),
            "a collection property sees both beans",
        )
    }

    @Test
    fun `an ambiguous type fails at the property rather than at startup`(@TempDir temp: Path) =
        runTest {
            val fixture = SeamFixture(temp)
            val api = fixture.owner(
                "surf-api",
                listOf(
                    definition(
                        LoudGreeter::class.java.name,
                        provides = greeterTypes(LoudGreeter::class.java.name),
                    ),
                    definition(Greeter::class.java.name),
                ),
            )

            install(fixture.host)

            // Both beans build: a container only refuses an ambiguity where something has to choose,
            // and nothing in the graph asks for a `Greeter`.
            fixture.host.startRoot(api)

            val holder = Holder()
            val failure = assertFailsWith<IllegalStateException> { holder.greeter }
            val message = failure.message.orEmpty()

            assertContains(message, "@Primary")
            assertContains(message, LoudGreeter::class.java.name)
        }

    // --- which container answers ----------------------------------------------------------------

    @Test
    fun `of names the container an application was given`(@TempDir temp: Path) = runTest {
        val fixture = SeamFixture(temp)
        val api = fixture.owner("surf-api", listOf(definition(Greeter::class.java.name)))
        val plugin = fixture.owner("plugin")
        val stranger = fixture.owner("stranger")

        install(fixture.host)
        fixture.host.startRoot(api)
        val child = fixture.host.startChild(plugin)

        assertSame(child, SurfContainer.of(plugin))
        assertNull(SurfContainer.of(stranger), "an application without a container has none")
    }

    /** Installs [host] and records it so [uninstallHost] can take it back out. */
    private fun install(host: SurfContainerHost) {
        SurfContainerAccess.install(host)
        installed = host
    }
}

/**
 * The kind of class every converted call site now is: a declaration that names a bean and is read
 * later.
 *
 * Written as a class rather than as top-level properties so each case gets a fresh, unresolved
 * delegate — a top-level property is initialised once per JVM, which would make the first case to
 * run decide what the rest see.
 */
private class Holder {

    val greeter by surfBean<Greeter>()

    val greeters by surfBeans<Greeter>()
}

/** A bean with nothing to it but an answer, so a case can tell which instance it got. */
private open class Greeter {

    open fun greet() = "hello"
}

/** A second bean of the same type, which is what makes a resolution ambiguous. */
private class LoudGreeter : Greeter() {

    override fun greet() = "HELLO"
}

/**
 * The shape that caused a real cycle: a class whose initialiser resolves a bean.
 *
 * Written as a companion property read during the companion's own initialisation, which is what
 * `companion object : Foo by someBean` compiles to — delegation stores the delegate, so it has to
 * read it. The container initialises this class in order to construct it, so the read happens
 * while the container is building this very bean.
 */
private class EagerGreeter : Greeter() {

    companion object {
        @Suppress("unused")
        val captured: Greeter = eagerlyResolved
    }
}

/** A second one, so each case gets a class the JVM has not already marked as failed. */
private class SecondEagerGreeter : Greeter() {

    companion object {
        @Suppress("unused")
        val captured: Greeter = eagerlyResolved
    }
}

/** Read during the eager companions' initialisation, which is the whole point. */
private val eagerlyResolved: Greeter by surfBean<Greeter>()

/** The types a `Greeter` subclass provides, which is how the real metadata would describe it. */
private fun greeterTypes(className: String) = listOf(className, Greeter::class.java.name)

/** Builds a host whose owners each serve their own metadata directory out of [temp]. */
private class SeamFixture(private val temp: Path) {

    private val classLoaders = mutableMapOf<Any, ClassLoader>()

    val host = SurfContainerHost(SeamPlatform(classLoaders))

    /** Registers an owner named [name] carrying one metadata file per entry of [files]. */
    fun owner(name: String, vararg files: List<BeanDefinition>): Any {
        val owner = SeamOwner(name)
        val directory = Files.createDirectories(temp.resolve(name).resolve(BeanMetadata.DIRECTORY))

        classLoaders[owner] = SeamClassLoader(temp.resolve(name))

        files.forEachIndexed { index, beans ->
            Files.writeString(
                directory.resolve("%02d-module.json".format(index)),
                BeanMetadata.json.encodeToString(BeanMetadata.serializer(), BeanMetadata(beans)),
            )
        }

        return owner
    }
}

/** An application the host knows only by identity; the name is what a failure message shows. */
private class SeamOwner(private val name: String) {

    override fun toString() = name
}

/** Answers the platform's questions from what the fixture registered, and nothing else. */
private class SeamPlatform(private val classLoaders: Map<Any, ClassLoader>) : ContainerPlatform {

    override fun classLoader(owner: Any): ClassLoader = requireNotNull(classLoaders[owner]) {
        "$owner was never registered with the fixture, so it has no metadata"
    }

    override fun beanSources(owner: Any): List<URL> =
        classLoader(owner).getResources(BeanMetadata.DIRECTORY).toList()

    override fun isPluginLoaded(pluginId: String) = false

    override fun logger(owner: Any): PlatformLogger = SilentLogger

    override fun dataPath(owner: Any): Path = Path.of("build", "test-data")

    override fun environments(): Set<String> = emptySet()

    override fun property(owner: Any, key: List<String>, file: String): String? = null
}

/**
 * Serves one owner's metadata directory while loading classes from the test's own loader.
 *
 * The host reads metadata *and* builds the beans by reflection, so its loader has to find the
 * classes declared in this file while serving only the resources this fixture wrote. Inheriting the
 * test classpath's own metadata would make what a case sees depend on the rest of the repository.
 */
private class SeamClassLoader(private val root: Path) :
    ClassLoader(SeamClassLoader::class.java.classLoader) {

    override fun getResources(name: String): Enumeration<URL> =
        if (name == BeanMetadata.DIRECTORY) {
            Collections.enumeration(listOf(root.resolve(name).toUri().toURL()))
        } else {
            super.getResources(name)
        }
}

/** Drops what the container reports; no case here reads it. */
private object SilentLogger : PlatformLogger {

    override fun info(message: String) = Unit

    override fun warn(message: String) = Unit

    override fun error(message: String, throwable: Throwable?) = Unit

    override fun debug(message: String) = Unit
}
